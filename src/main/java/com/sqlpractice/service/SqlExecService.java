package com.sqlpractice.service;

import com.sqlpractice.common.BizException;
import com.sqlpractice.common.Caps;
import com.sqlpractice.config.AppProperties;
import com.sqlpractice.dto.SqlDtos;
import com.sqlpractice.security.PermissionGuard;
import com.sqlpractice.util.SqlAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
@RequiredArgsConstructor
public class SqlExecService {

    private final QueryExecutor executor;
    private final PermissionGuard guard;
    private final AuditService auditService;
    private final AppProperties props;

    public SqlDtos.SqlExecuteResult execute(SqlDtos.SqlExecuteRequest req) {
        guard.requireCap(Caps.QUERY);
        guard.requireSchema(req.getSchema());

        SqlAnalyzer.Analysis analysis = SqlAnalyzer.analyze(req.getSql());
        if (SqlAnalyzer.DML.equals(analysis.kind())) {
            throw BizException.forbidden("平台不允许通过自定义 SQL 修改数据（" + analysis.statementType() + "）");
        }
        if (!SqlAnalyzer.READONLY.equals(analysis.kind()) && !SqlAnalyzer.DDL.equals(analysis.kind())) {
            throw BizException.badRequest("平台不支持该类型的语句");
        }

        guard.requireCap(Caps.RAW_SQL);
        if (SqlAnalyzer.DDL.equals(analysis.kind())) {
            guard.requireCap(analysis.capability());
            guard.requireDdlSchema(req.getSchema());
        }

        long start = System.currentTimeMillis();
        SqlDtos.SqlExecuteResult result = new SqlDtos.SqlExecuteResult();
        try {
            if (SqlAnalyzer.READONLY.equals(analysis.kind())) {
                QueryExecutor.ExecResult exec = executor.query(req.getSql(), props.getMaxResultRows(), props.getQueryTimeoutSeconds());
                result.setType("SELECT");
                result.setStatementType(analysis.statementType());
                result.setColumns(exec.getColumns());
                result.setRows(exec.getRows());
                result.setTruncated(exec.isTruncated());
                result.setRowCount(exec.getRows().size());
                result.setElapsedMs(exec.getElapsedMs());
                result.setMessage(exec.isTruncated()
                        ? "结果超过 " + props.getMaxResultRows() + " 行，已截断"
                        : "查询成功");
            } else {
                QueryExecutor.ExecResult exec = executor.executeDdl(req.getSql(), 30);
                result.setType("DDL");
                result.setStatementType(analysis.statementType());
                result.setElapsedMs(exec.getElapsedMs());
                result.setMessage("执行成功");
                result.setRowCount(0);
            }
            auditService.log("RAW_SQL", req.getSchema(), null, req.getSql(), true,
                    result.getMessage(), result.getElapsedMs());
            return result;
        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - start;
            auditService.log("RAW_SQL", req.getSchema(), null, req.getSql(), false, e.getMessage(), elapsed);
            throw new BizException(4001, friendlyMessage(e));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            auditService.log("RAW_SQL", req.getSchema(), null, req.getSql(), false, e.getMessage(), elapsed);
            throw new BizException(4001, "执行失败：" + e.getMessage());
        }
    }

    private static String friendlyMessage(SQLException e) {
        String message = e.getMessage();
        if (message == null) {
            return "SQL 执行失败";
        }
        if (message.contains("max_execution_time") || message.contains("Query execution was interrupted")) {
            return "查询超时（超过 5 秒），请缩小查询范围";
        }
        return "SQL 执行失败：" + message;
    }
}
