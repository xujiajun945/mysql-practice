package com.sqlpractice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

public class DdlDtos {

    @Data
    public static class ColumnDef {
        @NotBlank(message = "字段名不能为空")
        private String name;
        @NotBlank(message = "字段类型不能为空")
        private String type;
        private Integer length;
        private Integer precision;
        private Integer scale;
        private Boolean unsigned;
        private Boolean nullable = true;
        /** NONE / NULL / LITERAL / CURRENT_TIMESTAMP */
        private String defaultType = "NONE";
        private String defaultValue;
        private Boolean autoIncrement = false;
        private Boolean primaryKey = false;
        private Boolean unique = false;
        private String comment;
    }

    @Data
    public static class CreateTableRequest {
        @NotBlank(message = "schema 不能为空")
        private String schema;
        @NotBlank(message = "表名不能为空")
        private String table;
        private String comment;
        private List<ColumnDef> columns = new ArrayList<>();
    }

    @Data
    public static class TableRef {
        @NotBlank(message = "schema 不能为空")
        private String schema;
        @NotBlank(message = "表名不能为空")
        private String table;
    }

    @Data
    public static class DropTableRequest {
        @NotBlank(message = "schema 不能为空")
        private String schema;
        @NotBlank(message = "表名不能为空")
        private String table;
        @NotBlank(message = "请再次输入表名以确认")
        private String confirmName;
    }

    @Data
    public static class AddColumnRequest {
        @NotBlank(message = "schema 不能为空")
        private String schema;
        @NotBlank(message = "表名不能为空")
        private String table;
        private ColumnDef column;
        /** END / FIRST / AFTER */
        private String position = "END";
        private String afterColumn;
    }

    @Data
    public static class DropColumnRequest {
        @NotBlank(message = "schema 不能为空")
        private String schema;
        @NotBlank(message = "表名不能为空")
        private String table;
        @NotBlank(message = "字段名不能为空")
        private String column;
    }

    @Data
    public static class AddIndexRequest {
        @NotBlank(message = "schema 不能为空")
        private String schema;
        @NotBlank(message = "表名不能为空")
        private String table;
        private String name;
        private List<String> columns = new ArrayList<>();
        private Boolean unique = false;
    }

    @Data
    public static class DropIndexRequest {
        @NotBlank(message = "schema 不能为空")
        private String schema;
        @NotBlank(message = "表名不能为空")
        private String table;
        @NotBlank(message = "索引名不能为空")
        private String indexName;
    }

    @Data
    public static class DdlResult {
        private String sql;
        private String message;
    }
}
