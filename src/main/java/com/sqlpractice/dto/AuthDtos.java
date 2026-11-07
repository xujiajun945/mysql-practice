package com.sqlpractice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public class AuthDtos {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 30, message = "用户名长度需在 3-30 之间")
        private String username;
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 60, message = "密码长度需在 6-60 之间")
        private String password;
        private String nickname;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank(message = "refreshToken 不能为空")
        private String refreshToken;
    }

    @Data
    public static class LogoutRequest {
        private String refreshToken;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 60, message = "密码长度需在 6-60 之间")
        private String newPassword;
    }

    @Data
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private long expiresIn;
        private UserView user;
    }

    @Data
    public static class UserView {
        private Long id;
        private String username;
        private String nickname;
        private String status;
        private String roleCode;
        private String roleName;
        private List<String> capabilities;
        private List<String> schemas;
        private String rejectReason;
    }

    @Data
    public static class StatusView {
        private String username;
        private String status;
        private String rejectReason;
    }
}
