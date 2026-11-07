package com.sqlpractice.controller;

import com.sqlpractice.common.ApiResult;
import com.sqlpractice.dto.AdminDtos;
import com.sqlpractice.entity.Role;
import com.sqlpractice.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RoleService roleService;

    @GetMapping
    public ApiResult<List<Role>> list() {
        return ApiResult.ok(roleService.list());
    }

    @PostMapping
    public ApiResult<Role> create(@Valid @RequestBody AdminDtos.RoleRequest req) {
        return ApiResult.ok(roleService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResult<Role> update(@PathVariable Long id, @Valid @RequestBody AdminDtos.RoleRequest req) {
        return ApiResult.ok(roleService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResult.ok();
    }
}
