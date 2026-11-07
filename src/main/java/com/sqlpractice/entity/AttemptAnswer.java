package com.sqlpractice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttemptAnswer {
    private Long id;
    private Long attemptId;
    private Long questionId;
    private String userSql;
    private String autoResult;
    private String autoMessage;
    private String userResultJson;
    private String expectedResultJson;
    private Integer elapsedMs;
    private Integer correct;
    private String reviewComment;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime updatedAt;
}
