package com.sqlpractice.controller;

import com.sqlpractice.common.ApiResult;
import com.sqlpractice.dto.AuthDtos;
import com.sqlpractice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResult<AuthDtos.UserView> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return ApiResult.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResult<AuthDtos.TokenPair> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return ApiResult.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ApiResult<AuthDtos.TokenPair> refresh(@Valid @RequestBody AuthDtos.RefreshRequest req) {
        return ApiResult.ok(authService.refresh(req.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestBody(required = false) AuthDtos.LogoutRequest req) {
        authService.logout(req == null ? null : req.getRefreshToken());
        return ApiResult.ok();
    }
}
