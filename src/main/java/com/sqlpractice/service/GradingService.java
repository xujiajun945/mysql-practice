package com.sqlpractice.service;

import com.sqlpractice.common.BizException;
import com.sqlpractice.config.AppProperties;
import com.sqlpractice.util.SqlAnalyzer;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradingService {

    private static final String ROW_SEPARATOR = "\u0001";

    private final QueryExecutor executor;
    private final AppProperties props;

    @Data
    @Builder
    public static class GradeOutcome {
        private String result;
        private String message;
        private long elapsedMs;
        private List<String> userColumns;
        private List<List<String>> userRows;
        private List<String> expectedColumns;
        private List<List<String>> expectedRows;
        private boolean userTruncated;
        private boolean expectedTruncated;
    }

    public GradeOutcome grade(String userSql, String referenceSql, boolean orderSensitive) {
        SqlAnalyzer.Analysis analysis;
        try {
            analysis = SqlAnalyzer.analyze(userSql);
        } catch (BizException e) {
            return GradeOutcome.builder()
                    .result("NOT_SELECT")
                    .message("只能提交 SELECT 查询：" + e.getMessage())
                    .build();
        }
        if (!SqlAnalyzer.READONLY.equals(analysis.kind()) || !"SELECT".equals(analysis.statementType())) {
            return GradeOutcome.builder()
                    .result("NOT_SELECT")
                    .message("只能提交 SELECT 查询语句")
                    .build();
        }

        QueryExecutor.ExecResult expected;
        try {
            expected = executor.query(referenceSql, props.getGradingRowsLimit(), props.getQueryTimeoutSeconds());
        } catch (SQLException e) {
            throw new BizException(500, "题库参考 SQL 执行失败：" + e.getMessage());
        }

        QueryExecutor.ExecResult actual;
        try {
            actual = executor.query(userSql, props.getGradingRowsLimit(), props.getQueryTimeoutSeconds());
        } catch (SQLException e) {
            return GradeOutcome.builder()
                    .result(isTimeout(e) ? "TIMEOUT" : "RUNTIME_ERROR")
                    .message(isTimeout(e) ? "执行超时（超过 " + props.getQueryTimeoutSeconds() + " 秒）"
                            : e.getMessage())
                    .expectedColumns(expected.getColumns())
                    .expectedRows(expected.getRows())
                    .expectedTruncated(expected.isTruncated())
                    .build();
        }

        CompareResult compare = compare(expected, actual, orderSensitive);
        return GradeOutcome.builder()
                .result(compare.pass ? "PASS" : "WRONG_ANSWER")
                .message(compare.message)
                .elapsedMs(actual.getElapsedMs())
                .userColumns(actual.getColumns())
                .userRows(actual.getRows())
                .expectedColumns(expected.getColumns())
                .expectedRows(expected.getRows())
                .userTruncated(actual.isTruncated())
                .expectedTruncated(expected.isTruncated())
                .build();
    }

    private record CompareResult(boolean pass, String message) {
    }

    @Data
    @Builder
    public static class RunOutcome {
        private boolean ok;
        private String message;
        private long elapsedMs;
        private List<String> columns;
        private List<List<String>> rows;
        private boolean truncated;
        private int rowCount;
    }

    /** 做题页「测试」用：只执行不判题、不落库 */
    public RunOutcome run(String sql) {
        SqlAnalyzer.Analysis analysis;
        try {
            analysis = SqlAnalyzer.analyze(sql);
        } catch (BizException e) {
            return RunOutcome.builder().ok(false).message("只能执行 SELECT 查询：" + e.getMessage()).build();
        }
        if (!SqlAnalyzer.READONLY.equals(analysis.kind()) || !"SELECT".equals(analysis.statementType())) {
            return RunOutcome.builder().ok(false).message("只能执行 SELECT 查询语句").build();
        }
        QueryExecutor.ExecResult actual;
        try {
            actual = executor.query(sql, props.getGradingRowsLimit(), props.getQueryTimeoutSeconds());
        } catch (SQLException e) {
            return RunOutcome.builder()
                    .ok(false)
                    .message(isTimeout(e) ? "执行超时（超过 " + props.getQueryTimeoutSeconds() + " 秒）" : e.getMessage())
                    .build();
        }
        return RunOutcome.builder()
                .ok(true)
                .message("执行成功")
                .elapsedMs(actual.getElapsedMs())
                .columns(actual.getColumns())
                .rows(actual.getRows())
                .truncated(actual.isTruncated())
                .rowCount(actual.getRows().size())
                .build();
    }

    private CompareResult compare(QueryExecutor.ExecResult expected, QueryExecutor.ExecResult actual,
                                  boolean orderSensitive) {
        List<String> expectedColumns = expected.getColumns();
        List<String> actualColumns = actual.getColumns();
        if (expectedColumns.size() != actualColumns.size()) {
            return new CompareResult(false,
                    "列数不一致：期望 " + expectedColumns.size() + " 列，你的结果 " + actualColumns.size() + " 列");
        }
        List<List<String>> expectedRows = new ArrayList<>(expected.getRows());
        List<List<String>> actualRows = new ArrayList<>(actual.getRows());
        if (expectedRows.size() != actualRows.size()) {
            return new CompareResult(false,
                    "行数不一致：期望 " + expectedRows.size() + " 行，你的结果 " + actualRows.size() + " 行"
                            + (expected.isTruncated() || actual.isTruncated() ? "（结果已截断，请缩小范围）" : ""));
        }
        List<String> expectedKeys = toKeys(expectedRows, orderSensitive);
        List<String> actualKeys = toKeys(actualRows, orderSensitive);
        for (int i = 0; i < expectedKeys.size(); i++) {
            if (!expectedKeys.get(i).equals(actualKeys.get(i))) {
                String position = orderSensitive ? "第 " + (i + 1) + " 行" : "结果集中存在不匹配的行";
                return new CompareResult(false,
                        "结果不一致（" + position + "）\n期望：" + readableRow(expectedRows.get(i))
                                + "\n你的：" + readableRow(actualRows.get(i)));
            }
        }
        return new CompareResult(true, "结果集完全匹配");
    }

    private List<String> toKeys(List<List<String>> rows, boolean ordered) {
        List<String> keys = new ArrayList<>();
        for (List<String> row : rows) {
            StringBuilder sb = new StringBuilder();
            for (String cell : row) {
                sb.append(cell == null ? "\u0002NULL" : cell).append(ROW_SEPARATOR);
            }
            keys.add(sb.toString());
        }
        if (!ordered) {
            keys.sort(Comparator.naturalOrder());
        }
        return keys;
    }

    private String readableRow(List<String> row) {
        List<String> values = new ArrayList<>();
        for (String cell : row) {
            values.add(cell == null ? "NULL" : cell);
        }
        return "(" + String.join(" | ", values) + ")";
    }

    private boolean isTimeout(SQLException e) {
        String message = e.getMessage();
        return message != null && (message.contains("max_execution_time")
                || message.contains("Query execution was interrupted")
                || message.contains("Statement cancelled"));
    }

    /** 供题库自检使用：直接执行一条参考 SQL */
    public List<List<String>> runReference(String sql, int maxRows) throws SQLException {
        QueryExecutor.ExecResult result = executor.query(sql, maxRows, props.getQueryTimeoutSeconds());
        return result.getRows();
    }
}
