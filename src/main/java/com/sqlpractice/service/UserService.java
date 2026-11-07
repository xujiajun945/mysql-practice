package com.sqlpractice.service;

import com.sqlpractice.common.BizException;
import com.sqlpractice.common.Caps;
import com.sqlpractice.dto.AdminDtos;
import com.sqlpractice.entity.Role;
import com.sqlpractice.entity.User;
import com.sqlpractice.mapper.RoleMapper;
import com.sqlpractice.mapper.UserMapper;
import com.sqlpractice.security.PermissionGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionGuard guard;
    private final BCryptPasswordEncoder encoder;

    public List<AdminDtos.UserRow> list() {
        guard.requireCap(Caps.USER_ADMIN);
        Map<Long, Role> roles = new HashMap<>();
        for (Role role : roleMapper.findAll()) {
            roles.put(role.getId(), role);
        }
        return userMapper.findAll().stream().map(user -> {
            AdminDtos.UserRow row = new AdminDtos.UserRow();
            row.setId(user.getId());
            row.setUsername(user.getUsername());
            row.setNickname(user.getNickname());
            row.setRoleId(user.getRoleId());
            Role role = roles.get(user.getRoleId());
            if (role != null) {
                row.setRoleCode(role.getCode());
                row.setRoleName(role.getName());
            }
            row.setStatus(user.getStatus());
            row.setRejectReason(user.getRejectReason());
            row.setCreatedAt(user.getCreatedAt());
            row.setApprovedAt(user.getApprovedAt());
            row.setLastLoginAt(user.getLastLoginAt());
            return row;
        }).toList();
    }

    public void create(AdminDtos.CreateUserRequest req) {
        guard.requireCap(Caps.USER_ADMIN);
        if (userMapper.countByUsername(req.getUsername()) > 0) {
            throw BizException.badRequest("用户名已存在");
        }
        Role role = roleMapper.findById(req.getRoleId() == null ? 0L : req.getRoleId());
        if (role == null) {
            throw BizException.badRequest("角色不存在");
        }
        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() == null || req.getNickname().isBlank()
                ? req.getUsername() : req.getNickname().trim());
        user.setRoleId(role.getId());
        user.setStatus("ACTIVE");
        userMapper.insert(user);
    }

    public void approve(Long userId, AdminDtos.ApproveRequest req) {
        guard.requireCap(Caps.USER_ADMIN);
        User user = requireUser(userId);
        if (req != null && req.getRoleId() != null) {
            Role role = roleMapper.findById(req.getRoleId());
            if (role == null) {
                throw BizException.badRequest("角色不存在");
            }
            userMapper.updateRole(userId, role.getId());
        }
        userMapper.updateStatus(userId, "ACTIVE", null);
    }

    public void reject(Long userId, AdminDtos.RejectRequest req) {
        guard.requireCap(Caps.USER_ADMIN);
        requireUser(userId);
        String reason = req == null ? null : req.getReason();
        userMapper.updateStatus(userId, "REJECTED", reason);
    }

    public void update(Long userId, AdminDtos.UpdateUserRequest req) {
        guard.requireCap(Caps.USER_ADMIN);
        User user = requireUser(userId);
        if (req.getNickname() != null && !req.getNickname().isBlank()) {
            userMapper.updateNickname(userId, req.getNickname().trim());
        }
        if (req.getRoleId() != null && !req.getRoleId().equals(user.getRoleId())) {
            Role role = roleMapper.findById(req.getRoleId());
            if (role == null) {
                throw BizException.badRequest("角色不存在");
            }
            userMapper.updateRole(userId, role.getId());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            String status = req.getStatus().toUpperCase();
            if (!List.of("ACTIVE", "PENDING", "REJECTED", "DISABLED").contains(status)) {
                throw BizException.badRequest("状态不合法");
            }
            userMapper.updateStatus(userId, status, req.getRejectReason());
        }
    }

    public void resetPassword(Long userId, String newPassword) {
        guard.requireCap(Caps.USER_ADMIN);
        if (newPassword == null || newPassword.length() < 6) {
            throw BizException.badRequest("密码长度至少 6 位");
        }
        requireUser(userId);
        userMapper.updatePassword(userId, encoder.encode(newPassword));
    }

    private User requireUser(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return user;
    }
}
