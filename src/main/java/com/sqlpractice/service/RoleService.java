package com.sqlpractice.service;

import com.sqlpractice.common.BizException;
import com.sqlpractice.common.Caps;
import com.sqlpractice.dto.AdminDtos;
import com.sqlpractice.entity.Role;
import com.sqlpractice.mapper.RoleMapper;
import com.sqlpractice.mapper.UserMapper;
import com.sqlpractice.security.PermissionGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {

    private static final Set<String> VALID_CAPS = Set.of(
            Caps.QUERY, Caps.RAW_SQL, Caps.CREATE_TABLE, Caps.DROP_TABLE, Caps.ALTER_COLUMN,
            Caps.INDEX, Caps.REVIEW, Caps.USER_ADMIN, Caps.QUESTION_ADMIN, Caps.AUDIT_VIEW);

    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final PermissionGuard guard;

    public List<Role> list() {
        guard.requireCap(Caps.USER_ADMIN);
        return roleMapper.findAll();
    }

    public Role create(AdminDtos.RoleRequest req) {
        guard.requireCap(Caps.USER_ADMIN);
        if (roleMapper.findByCode(req.getCode()) != null) {
            throw BizException.badRequest("角色编码已存在");
        }
        Role role = new Role();
        role.setCode(req.getCode().trim().toUpperCase());
        role.setName(req.getName().trim());
        role.setCapabilities(normalizeCaps(req.getCapabilities()));
        role.setAllowedSchemas(normalizeSchemas(req.getAllowedSchemas()));
        role.setBuiltIn(0);
        roleMapper.insert(role);
        return role;
    }

    public Role update(Long id, AdminDtos.RoleRequest req) {
        guard.requireCap(Caps.USER_ADMIN);
        Role role = roleMapper.findById(id);
        if (role == null) {
            throw BizException.notFound("角色不存在");
        }
        role.setName(req.getName().trim());
        role.setCapabilities(normalizeCaps(req.getCapabilities()));
        role.setAllowedSchemas(normalizeSchemas(req.getAllowedSchemas()));
        roleMapper.update(role);
        return role;
    }

    public void delete(Long id) {
        guard.requireCap(Caps.USER_ADMIN);
        Role role = roleMapper.findById(id);
        if (role == null) {
            throw BizException.notFound("角色不存在");
        }
        if (role.getBuiltIn() != null && role.getBuiltIn() == 1) {
            throw BizException.badRequest("内置角色不允许删除");
        }
        if (userMapper.countByRoleId(id) > 0) {
            throw BizException.badRequest("仍有用户使用该角色，无法删除");
        }
        roleMapper.deleteById(id);
    }

    private String normalizeCaps(String csv) {
        if (csv == null || csv.isBlank()) {
            return "";
        }
        List<String> caps = new ArrayList<>();
        for (String s : csv.split(",")) {
            String cap = s.trim().toUpperCase();
            if (cap.isEmpty()) {
                continue;
            }
            if (!VALID_CAPS.contains(cap)) {
                throw BizException.badRequest("未知权限位：" + cap);
            }
            caps.add(cap);
        }
        return String.join(",", caps);
    }

    private String normalizeSchemas(String csv) {
        if (csv == null || csv.isBlank()) {
            return "";
        }
        List<String> schemas = new ArrayList<>();
        for (String s : csv.split(",")) {
            String schema = s.trim();
            if (schema.isEmpty()) {
                continue;
            }
            if (!schema.matches("^[A-Za-z0-9_$]{1,64}$")) {
                throw BizException.badRequest("schema 名称不合法：" + schema);
            }
            schemas.add(schema);
        }
        return String.join(",", schemas);
    }
}
