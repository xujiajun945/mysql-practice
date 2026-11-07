package com.sqlpractice.security;

import com.sqlpractice.common.BizException;
import com.sqlpractice.entity.Role;
import com.sqlpractice.entity.User;
import com.sqlpractice.mapper.RoleMapper;
import com.sqlpractice.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw BizException.unauthorized("未登录或登录已失效");
        }
        String token = header.substring(7).trim();
        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw BizException.unauthorized("令牌无效或已过期");
        }
        if (!JwtService.TYPE_ACCESS.equals(claims.get("typ", String.class))) {
            throw BizException.unauthorized("请使用访问令牌");
        }

        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw BizException.unauthorized("令牌格式错误");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            throw BizException.unauthorized("用户不存在");
        }
        if ("PENDING".equals(user.getStatus())) {
            throw BizException.forbidden("账号待管理员审批，暂时无法使用");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw BizException.forbidden("账号状态为 " + user.getStatus() + "，无法使用");
        }

        Role role = roleMapper.findById(user.getRoleId());
        if (role == null) {
            throw BizException.forbidden("角色不存在，请联系管理员");
        }

        UserContext.set(CurrentUser.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .roleId(role.getId())
                .roleCode(role.getCode())
                .roleName(role.getName())
                .capabilities(split(role.getCapabilities()))
                .schemas(split(role.getAllowedSchemas()))
                .status(user.getStatus())
                .ip(clientIp(request))
                .build());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private static Set<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
