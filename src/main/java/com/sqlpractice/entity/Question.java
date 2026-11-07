package com.sqlpractice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Question {
    private Long id;
    private String title;
    private String description;
    private String difficulty;
    private String tags;
    private String referenceSql;
    private String explanation;
    private Integer orderSensitive;
    private Integer enabled;
    private LocalDateTime createdAt;
}
