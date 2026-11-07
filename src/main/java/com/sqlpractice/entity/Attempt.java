package com.sqlpractice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Attempt {
    private Long id;
    private Long userId;
    private String status;
    private String questionIds;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long reviewerId;
    private String overallComment;
    private Integer score;
}
