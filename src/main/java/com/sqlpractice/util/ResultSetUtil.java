package com.sqlpractice.util;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ResultSetUtil {

    private ResultSetUtil() {
    }

    public static List<String> columns(ResultSetMetaData md) throws SQLException {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            list.add(md.getColumnLabel(i));
        }
        return list;
    }

    public static List<List<String>> readRows(ResultSet rs, int maxRows, AtomicBoolean truncated) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        int columnCount = rs.getMetaData().getColumnCount();
        while (rs.next()) {
            if (rows.size() >= maxRows) {
                truncated.set(true);
                break;
            }
            List<String> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(cell(rs.getObject(i)));
            }
            rows.add(row);
        }
        return rows;
    }

    /** 把 JDBC 取出的值转成稳定、可直接比较的字符串 */
    public static String cell(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().toString().replace('T', ' ');
        }
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate().toString();
        }
        if (value instanceof java.sql.Time t) {
            return t.toLocalTime().toString();
        }
        if (value instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Double || value instanceof Float) {
            return BigDecimal.valueOf(((Number) value).doubleValue()).stripTrailingZeros().toPlainString();
        }
        if (value instanceof java.time.LocalDateTime dt) {
            return dt.toString().replace('T', ' ');
        }
        if (value instanceof Boolean b) {
            return b ? "1" : "0";
        }
        return String.valueOf(value);
    }
}
