package com.sqlpractice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private Long roleId;
    private String status;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime lastLoginAt;
}
