package com.sqlpractice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

public class SqlDtos {

    @Data
    public static class SqlExecuteRequest {
        @NotBlank(message = "schema 不能为空")
        private String schema;
        @NotBlank(message = "SQL 不能为空")
        private String sql;
    }

    @Data
    public static class SqlExecuteResult {
        /** SELECT / DDL / DML */
        private String type;
        private String statementType;
        private List<String> columns;
        private List<List<String>> rows;
        private Integer updateCount;
        private long elapsedMs;
        private boolean truncated;
        private int rowCount;
        private String message;
    }
}
