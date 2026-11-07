package com.sqlpractice.controller;

import com.sqlpractice.common.ApiResult;
import com.sqlpractice.dto.AdminDtos;
import com.sqlpractice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ApiResult<List<AdminDtos.UserRow>> list() {
        return ApiResult.ok(userService.list());
    }

    @PostMapping
    public ApiResult<Void> create(@Valid @RequestBody AdminDtos.CreateUserRequest req) {
        userService.create(req);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/approve")
    public ApiResult<Void> approve(@PathVariable Long id,
                                   @RequestBody(required = false) AdminDtos.ApproveRequest req) {
        userService.approve(id, req);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/reject")
    public ApiResult<Void> reject(@PathVariable Long id,
                                  @RequestBody(required = false) AdminDtos.RejectRequest req) {
        userService.reject(id, req);
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody AdminDtos.UpdateUserRequest req) {
        userService.update(id, req);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/password")
    public ApiResult<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return ApiResult.ok();
    }
}
