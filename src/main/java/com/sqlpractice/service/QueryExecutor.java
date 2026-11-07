package com.sqlpractice.service;

import com.sqlpractice.config.AppProperties;
import com.sqlpractice.util.ResultSetUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class QueryExecutor {

    @Qualifier("roDataSource")
    private final DataSource roDataSource;

    @Qualifier("ddlDataSource")
    private final DataSource ddlDataSource;

    private final AppProperties props;

    public QueryExecutor(@Qualifier("roDataSource") DataSource roDataSource,
                         @Qualifier("ddlDataSource") DataSource ddlDataSource,
                         AppProperties props) {
        this.roDataSource = roDataSource;
        this.ddlDataSource = ddlDataSource;
        this.props = props;
    }

    @Data
    public static class ExecResult {
        private List<String> columns = new ArrayList<>();
        private List<List<String>> rows = new ArrayList<>();
        private boolean truncated;
        private long elapsedMs;
        private int updateCount;
    }

    public ExecResult query(String sql, int maxRows, int timeoutSeconds) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = roDataSource.getConnection()) {
            conn.setAutoCommit(true);
            try (Statement set = conn.createStatement()) {
                set.execute("SET SESSION max_execution_time=" + timeoutSeconds * 1000L);
            }
            try (Statement st = conn.createStatement()) {
                st.setQueryTimeout(timeoutSeconds);
                boolean hasResultSet = st.execute(sql);
                ExecResult result = new ExecResult();
                if (hasResultSet) {
                    try (ResultSet rs = st.getResultSet()) {
                        result.setColumns(ResultSetUtil.columns(rs.getMetaData()));
                        AtomicBoolean truncated = new AtomicBoolean(false);
                        result.setRows(ResultSetUtil.readRows(rs, maxRows, truncated));
                        result.setTruncated(truncated.get());
                    }
                } else {
                    result.setUpdateCount(st.getUpdateCount());
                }
                result.setElapsedMs(System.currentTimeMillis() - start);
                return result;
            }
        }
    }

    public ExecResult executeDdl(String sql, int timeoutSeconds) throws SQLException {
        long start = System.currentTimeMillis();
        try (Connection conn = ddlDataSource.getConnection();
             Statement st = conn.createStatement()) {
            st.setQueryTimeout(timeoutSeconds);
            st.execute(sql);
            ExecResult result = new ExecResult();
            result.setElapsedMs(System.currentTimeMillis() - start);
            return result;
        }
    }

    public boolean pingRo() {
        try (Connection conn = roDataSource.getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
