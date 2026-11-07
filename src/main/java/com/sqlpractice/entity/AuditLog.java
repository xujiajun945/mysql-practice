package com.sqlpractice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLog {
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
