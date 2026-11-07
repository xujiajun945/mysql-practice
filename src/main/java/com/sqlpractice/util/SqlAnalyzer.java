package com.sqlpractice.util;

import com.sqlpractice.common.BizException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * SQL 语句类型判定。注意：这只是第一道提示性防线，
 * 真正兜底的是数据库账号权限（只读账号 / 仅沙盒 DDL 账号）。
 */
public final class SqlAnalyzer {

    public static final String READONLY = "READONLY";
    public static final String DDL = "DDL";
    public static final String DML = "DML";
    public static final String OTHER = "OTHER";

    private SqlAnalyzer() {
    }

    public record Analysis(String kind, String statementType, String capability) {
    }

    public static Analysis analyze(String rawSql) {
        if (!StringUtils.hasText(rawSql)) {
            throw BizException.badRequest("SQL 不能为空");
        }
        String sql = rawSql.trim();
        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        if (sql.isEmpty()) {
            throw BizException.badRequest("SQL 不能为空");
        }
        if (countStatements(sql) > 1) {
            throw BizException.badRequest("一次只能执行一条 SQL 语句");
        }

        String first = firstWord(sql);
        if (first.equals("SHOW") || first.equals("DESC") || first.equals("DESCRIBE")
                || first.equals("EXPLAIN") || first.equals("HELP")) {
            return new Analysis(READONLY, first, null);
        }
        if (first.equals("WITH")) {
            return new Analysis(READONLY, "SELECT", null);
        }

        Statement statement;
        try {
            Statements statements = CCJSqlParserUtil.parseStatements(sql);
            if (statements.getStatements().size() != 1) {
                throw BizException.badRequest("一次只能执行一条 SQL 语句");
            }
            statement = statements.getStatements().get(0);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw BizException.badRequest("SQL 解析失败：" + e.getMessage());
        }

        if (statement instanceof Select) {
            return new Analysis(READONLY, "SELECT", null);
        }
        if (statement instanceof Insert) {
            return new Analysis(DML, "INSERT", null);
        }
        if (statement instanceof Update) {
            return new Analysis(DML, "UPDATE", null);
        }
        if (statement instanceof Delete) {
            return new Analysis(DML, "DELETE", null);
        }
        if (statement instanceof Merge) {
            return new Analysis(DML, "MERGE", null);
        }
        if (statement instanceof CreateTable) {
            return new Analysis(DDL, "CREATE TABLE", "CREATE_TABLE");
        }
        if (statement instanceof CreateIndex) {
            return new Analysis(DDL, "CREATE INDEX", "INDEX");
        }
        if (statement instanceof CreateView) {
            throw BizException.badRequest("平台不支持创建视图");
        }
        if (statement instanceof Alter) {
            String upper = sql.toUpperCase(Locale.ROOT);
            boolean indexOp = upper.contains("ADD INDEX") || upper.contains("ADD KEY")
                    || upper.contains("ADD UNIQUE") || upper.contains("DROP INDEX")
                    || upper.contains("DROP KEY") || upper.contains("ADD PRIMARY KEY")
                    || upper.contains("ADD CONSTRAINT");
            return new Analysis(DDL, "ALTER TABLE", indexOp ? "INDEX" : "ALTER_COLUMN");
        }
        if (statement instanceof Drop) {
            String upper = sql.toUpperCase(Locale.ROOT);
            if (upper.startsWith("DROP INDEX")) {
                return new Analysis(DDL, "DROP INDEX", "INDEX");
            }
            if (upper.startsWith("DROP TABLE")) {
                return new Analysis(DDL, "DROP TABLE", "DROP_TABLE");
            }
            throw BizException.badRequest("平台只允许 DROP TABLE / DROP INDEX");
        }
        if (sql.toUpperCase(Locale.ROOT).startsWith("TRUNCATE")) {
            throw BizException.badRequest("平台不支持 TRUNCATE，请用 DROP TABLE 或明确的数据变更流程");
        }
        return new Analysis(OTHER, "OTHER", null);
    }

    private static String firstWord(String sql) {
        int i = 0;
        while (i < sql.length() && Character.isWhitespace(sql.charAt(i))) {
            i++;
        }
        int j = i;
        while (j < sql.length() && Character.isLetter(sql.charAt(j))) {
            j++;
        }
        return sql.substring(i, j).toUpperCase(Locale.ROOT);
    }

    /** 扫描引号外的分号，避免多语句注入 */
    private static int countStatements(String sql) {
        int count = 0;
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inBacktick = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\\' && (inSingle || inDouble)) {
                i++;
                continue;
            }
            if (c == '\'' && !inDouble && !inBacktick) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle && !inBacktick) {
                inDouble = !inDouble;
            } else if (c == '`' && !inSingle && !inDouble) {
                inBacktick = !inBacktick;
            } else if (c == ';' && !inSingle && !inDouble && !inBacktick) {
                if (sql.substring(i + 1).trim().length() > 0) {
                    count++;
                }
            }
        }
        return count;
    }
}
