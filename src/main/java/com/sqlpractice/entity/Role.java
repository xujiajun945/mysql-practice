package com.sqlpractice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Role {
    private Long id;
    private String code;
    private String name;
    private String capabilities;
    private String allowedSchemas;
    private Integer builtIn;
    private LocalDateTime createdAt;
}
