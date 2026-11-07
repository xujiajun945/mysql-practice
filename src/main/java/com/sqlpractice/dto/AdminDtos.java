package com.sqlpractice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

public class AdminDtos {

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String nickname;
        private Long roleId;
    }

    @Data
    public static class UpdateUserRequest {
        private String nickname;
        private Long roleId;
        private String status;
        private String rejectReason;
    }

    @Data
    public static class ApproveRequest {
        private Long roleId;
    }

    @Data
    public static class RejectRequest {
        private String reason;
    }

    @Data
    public static class RoleRequest {
        @NotBlank(message = "角色编码不能为空")
        private String code;
        @NotBlank(message = "角色名称不能为空")
        private String name;
        private String capabilities;
        private String allowedSchemas;
    }

    @Data
    public static class ReviewAnswerRequest {
        private Boolean correct;
        private String comment;
    }

    @Data
    public static class CompleteReviewRequest {
        private String overallComment;
    }

    @Data
    public static class QuestionRequest {
        @NotBlank(message = "标题不能为空")
        private String title;
        @NotBlank(message = "题目描述不能为空")
        private String description;
        private String difficulty;
        private String tags;
        @NotBlank(message = "参考 SQL 不能为空")
        private String referenceSql;
        private String explanation;
        private Integer orderSensitive;
        private Integer enabled;
    }

    @Data
    public static class UserRow {
        private Long id;
        private String username;
        private String nickname;
        private Long roleId;
        private String roleCode;
        private String roleName;
        private String status;
        private String rejectReason;
        private LocalDateTime createdAt;
        private LocalDateTime approvedAt;
        private LocalDateTime lastLoginAt;
    }

    @Data
    public static class AuditRow {
        private Long id;
        private Long userId;
        private String username;
        private String action;
        private String schemaName;
        private String tableName;
        private String sqlText;
        private Integer success;
        private String message;
        private Integer elapsedMs;
        private String ip;
        private LocalDateTime createdAt;
    }
}
