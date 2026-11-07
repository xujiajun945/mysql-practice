package com.sqlpractice.util;

import com.sqlpractice.common.BizException;

import java.util.regex.Pattern;

public final class SqlIdentifier {

    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_$]{1,64}$");

    private SqlIdentifier() {
    }

    public static String require(String name, String label) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw BizException.badRequest(label + "只能由字母、数字、下划线组成，长度 1-64");
        }
        return name;
    }

    public static String quote(String name) {
        return "`" + require(name, "标识符") + "`";
    }

    public static String qualified(String schema, String table) {
        return quote(schema) + "." + quote(table);
    }

    /** MySQL 字符串字面量：单引号转义 */
    public static String literal(String value) {
        return "'" + value.replace("'", "''").replace("\\", "\\\\") + "'";
    }
}
