package com.sqlpractice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class QuizDtos {

    @Data
    public static class SubmitAnswerRequest {
        @NotBlank(message = "SQL 不能为空")
        private String sql;
    }

    @Data
    public static class TestRunResult {
        private boolean ok;
        private String message;
        private List<String> columns;
        private List<List<String>> rows;
        private int rowCount;
        private boolean truncated;
        private long elapsedMs;
    }

    @Data
    public static class AnswerView {
        private Long id;
        private Long questionId;
        private String userSql;
        private String autoResult;
        private String autoMessage;
        private Integer elapsedMs;
        private Integer correct;
        private String reviewComment;
        /** 批阅后才下发 */
        private String referenceSql;
        private String explanation;
        private List<String> expectedColumns;
        private List<List<String>> expectedRows;
        private List<String> userColumns;
        private List<List<String>> userRows;
    }

    @Data
    public static class QuestionView {
        private Long id;
        private String title;
        private String description;
        private String difficulty;
        private String tags;
        private Integer orderSensitive;
        private AnswerView answer;
    }

    @Data
    public static class AttemptView {
        private Long id;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
        private Integer score;
        private String overallComment;
        private int total;
        private int answered;
        private List<QuestionView> questions;
    }

    @Data
    public static class AttemptSummary {
        private Long id;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
        private Integer score;
        private int total;
        private int answered;
    }

    @Data
    public static class ReviewQueueItem {
        private Long attemptId;
        private Long userId;
        private String username;
        private String nickname;
        private LocalDateTime submittedAt;
        private int total;
        private int answered;
        private int autoPass;
        private Integer score;
        private String status;
    }

    @Data
    public static class ReviewDetail {
        private Long attemptId;
        private Long userId;
        private String username;
        private String nickname;
        private String status;
        private LocalDateTime submittedAt;
        private String overallComment;
        private Integer score;
        private List<QuestionView> questions;
    }
}
