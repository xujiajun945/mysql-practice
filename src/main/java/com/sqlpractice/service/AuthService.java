package com.sqlpractice.service;

import com.sqlpractice.common.BizException;
import com.sqlpractice.config.AppProperties;
import com.sqlpractice.dto.AuthDtos;
import com.sqlpractice.entity.Role;
import com.sqlpractice.entity.User;
import com.sqlpractice.mapper.RoleMapper;
import com.sqlpractice.mapper.UserMapper;
import com.sqlpractice.security.CurrentUser;
import com.sqlpractice.security.JwtService;
import com.sqlpractice.security.PermissionGuard;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_KEY = "auth:refresh:%d:%s";
    private static final String USER_SET = "auth:user:refresh:%d";
    private static final String GRACE_KEY = "auth:grace:%s";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final JwtService jwtService;
    private final StringRedisTemplate redis;
    private final BCryptPasswordEncoder encoder;
    private final PermissionGuard guard;
    private final AppProperties props;

    public AuthDtos.UserView register(AuthDtos.RegisterRequest req) {
        if (userMapper.countByUsername(req.getUsername()) > 0) {
            throw BizException.badRequest("用户名已存在");
        }
        Role reader = roleMapper.findByCode("READER");
        if (reader == null) {
            throw new BizException(500, "系统未初始化默认角色，请检查服务启动日志");
        }
        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() == null || req.getNickname().isBlank()
                ? req.getUsername() : req.getNickname().trim());
        user.setRoleId(reader.getId());
        user.setStatus("PENDING");
        userMapper.insert(user);
        return toView(user, reader, "PENDING");
    }

    public AuthDtos.TokenPair login(AuthDtos.LoginRequest req) {
        User user = userMapper.findByUsername(req.getUsername().trim());
        if (user == null || !encoder.matches(req.getPassword(), user.getPassword())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        if ("DISABLED".equals(user.getStatus())) {
            throw BizException.forbidden("账号已被禁用");
        }
        if ("REJECTED".equals(user.getStatus())) {
            throw BizException.forbidden("注册申请已被拒绝" + (user.getRejectReason() == null ? "" : "：" + user.getRejectReason()));
        }
        Role role = roleMapper.findById(user.getRoleId());
        if (role == null) {
            throw BizException.forbidden("角色不存在，请联系管理员");
        }
        userMapper.touchLogin(user.getId());
        return issue(user, role);
    }

    public AuthDtos.TokenPair refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (Exception e) {
            throw BizException.unauthorized("刷新令牌无效或已过期");
        }
        if (!JwtService.TYPE_REFRESH.equals(claims.get("typ", String.class))) {
            throw BizException.unauthorized("请使用刷新令牌");
        }
        Long userId = Long.valueOf(claims.getSubject());
        String jti = claims.getId();
        String key = String.format(REFRESH_KEY, userId, jti);

        Boolean exists = redis.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            redis.delete(key);
            redis.opsForSet().remove(String.format(USER_SET, userId), jti);
            redis.opsForValue().set(String.format(GRACE_KEY, jti), "1", Duration.ofSeconds(15));
        } else if (Boolean.TRUE.equals(redis.hasKey(String.format(GRACE_KEY, jti)))) {
            // 并发刷新：同一令牌短时间内重复使用，视为同一次轮换
        } else {
            revokeAll(userId);
            throw BizException.unauthorized("登录状态已失效，请重新登录");
        }

        User user = userMapper.findById(userId);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw BizException.forbidden("账号不可用，请联系管理员");
        }
        Role role = roleMapper.findById(user.getRoleId());
        if (role == null) {
            throw BizException.forbidden("角色不存在，请联系管理员");
        }
        return issue(user, role);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtService.parse(refreshToken);
            Long userId = Long.valueOf(claims.getSubject());
            String jti = claims.getId();
            redis.delete(String.format(REFRESH_KEY, userId, jti));
            redis.opsForSet().remove(String.format(USER_SET, userId), jti);
        } catch (Exception ignored) {
            // 令牌本身已失效，无需处理
        }
    }

    public AuthDtos.UserView me() {
        CurrentUser current = guard.current();
        AuthDtos.UserView view = new AuthDtos.UserView();
        view.setId(current.getId());
        view.setUsername(current.getUsername());
        view.setNickname(current.getNickname());
        view.setStatus(current.getStatus());
        view.setRoleCode(current.getRoleCode());
        view.setRoleName(current.getRoleName());
        view.setCapabilities(new ArrayList<>(current.getCapabilities()));
        view.setSchemas(new ArrayList<>(current.getSchemas()));
        return view;
    }

    public AuthDtos.StatusView status(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        AuthDtos.StatusView view = new AuthDtos.StatusView();
        view.setUsername(user.getUsername());
        view.setStatus(user.getStatus());
        view.setRejectReason(user.getRejectReason());
        return view;
    }

    public void changePassword(AuthDtos.ChangePasswordRequest req) {
        CurrentUser current = guard.current();
        User user = userMapper.findById(current.getId());
        if (user == null || !encoder.matches(req.getOldPassword(), user.getPassword())) {
            throw BizException.badRequest("原密码不正确");
        }
        userMapper.updatePassword(user.getId(), encoder.encode(req.getNewPassword()));
    }

    private AuthDtos.TokenPair issue(User user, Role role) {
        String access = jwtService.createAccessToken(user);
        String refresh = jwtService.createRefreshToken(user);
        Claims claims = jwtService.parse(refresh);
        String jti = claims.getId();
        String key = String.format(REFRESH_KEY, user.getId(), jti);
        redis.opsForValue().set(key, "1", Duration.ofSeconds(jwtService.refreshTtlSeconds()));
        String userSet = String.format(USER_SET, user.getId());
        redis.opsForSet().add(userSet, jti);
        redis.expire(userSet, Duration.ofSeconds(jwtService.refreshTtlSeconds()));

        AuthDtos.TokenPair pair = new AuthDtos.TokenPair();
        pair.setAccessToken(access);
        pair.setRefreshToken(refresh);
        pair.setExpiresIn(props.getJwt().getAccessTtlMinutes() * 60);
        pair.setUser(toView(user, role, user.getStatus()));
        return pair;
    }

    private void revokeAll(Long userId) {
        Set<String> jtis = redis.opsForSet().members(String.format(USER_SET, userId));
        if (jtis != null) {
            for (String jti : jtis) {
                redis.delete(String.format(REFRESH_KEY, userId, jti));
            }
        }
        redis.delete(String.format(USER_SET, userId));
    }

    private AuthDtos.UserView toView(User user, Role role, String status) {
        AuthDtos.UserView view = new AuthDtos.UserView();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setNickname(user.getNickname());
        view.setStatus(status);
        view.setRoleCode(role.getCode());
        view.setRoleName(role.getName());
        view.setCapabilities(split(role.getCapabilities()));
        view.setSchemas(split(role.getAllowedSchemas()));
        view.setRejectReason(user.getRejectReason());
        return view;
    }

    private static List<String> split(String csv) {
        List<String> list = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return list;
        }
        for (String s : csv.split(",")) {
            if (!s.isBlank()) {
                list.add(s.trim());
            }
        }
        return list;
    }
}
