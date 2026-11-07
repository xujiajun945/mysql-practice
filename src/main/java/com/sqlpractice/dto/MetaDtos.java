package com.sqlpractice.dto;

import lombok.Data;

import java.util.List;

public class MetaDtos {

    @Data
    public static class SchemaInfo {
        private String name;
        private String charset;
    }

    @Data
    public static class TableInfo {
        private String name;
        private String type;
        private Long rowCount;
        private Double sizeKb;
        private String comment;
    }

    @Data
    public static class ColumnInfo {
        private String name;
        private String type;
        private String collation;
        private String nullable;
        private String key;
        private String defaultValue;
        private String extra;
        private String comment;
    }

    @Data
    public static class IndexInfo {
        private String name;
        private boolean unique;
        private List<String> columns;
        private String type;
    }

    @Data
    public static class TableDetail {
        private String schema;
        private String table;
        private String createSql;
        private List<ColumnInfo> columns;
        private List<IndexInfo> indexes;
        private Long rowCount;
        private Double sizeKb;
        private String comment;
    }

    @Data
    public static class PreviewResult {
        private List<String> columns;
        private List<List<String>> rows;
        private boolean truncated;
        private long elapsedMs;
        private int rowCount;
    }
}
