package com.sqlpractice.controller;

import com.sqlpractice.common.ApiResult;
import com.sqlpractice.dto.AuthDtos;
import com.sqlpractice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final AuthService authService;

    @GetMapping
    public ApiResult<AuthDtos.UserView> me() {
        return ApiResult.ok(authService.me());
    }

    @GetMapping("/status")
    public ApiResult<AuthDtos.StatusView> status(@RequestParam String username) {
        return ApiResult.ok(authService.status(username));
    }

    @PostMapping("/password")
    public ApiResult<Void> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest req) {
        authService.changePassword(req);
        return ApiResult.ok();
    }
}
