package com.sqlpractice.service;

import com.sqlpractice.common.BizException;
import com.sqlpractice.common.Caps;
import com.sqlpractice.config.AppProperties;
import com.sqlpractice.dto.DdlDtos;
import com.sqlpractice.security.PermissionGuard;
import com.sqlpractice.util.SqlIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DdlService {

    private static final Set<String> INT_TYPES = Set.of("tinyint", "smallint", "mediumint", "int", "bigint");
    private static final Set<String> NUMERIC_TYPES = Set.of("tinyint", "smallint", "mediumint", "int",
            "bigint", "decimal", "float", "double");
    private static final Set<String> NO_LITERAL_DEFAULT = Set.of("tinytext", "text", "mediumtext", "longtext",
            "tinyblob", "blob", "mediumblob", "longblob", "json");
    private static final Set<String> TIME_DEFAULT_TYPES = Set.of("datetime", "timestamp");

    @Qualifier("ddlJdbcTemplate")
    private final JdbcTemplate ddlJdbc;
    private final MetaService metaService;
    private final PermissionGuard guard;
    private final AuditService auditService;
    private final AppProperties props;

    public DdlService(@Qualifier("ddlJdbcTemplate") JdbcTemplate ddlJdbc,
                      MetaService metaService,
                      PermissionGuard guard,
                      AuditService auditService,
                      AppProperties props) {
        this.ddlJdbc = ddlJdbc;
        this.metaService = metaService;
        this.guard = guard;
        this.auditService = auditService;
        this.props = props;
    }

    // ---------------------------------------------------------------- 建表

    public DdlDtos.DdlResult previewCreateTable(DdlDtos.CreateTableRequest req) {
        DdlDtos.DdlResult result = new DdlDtos.DdlResult();
        result.setSql(buildCreateSql(req));
        result.setMessage("预览：确认无误后点击执行");
        return result;
    }

    public DdlDtos.DdlResult createTable(DdlDtos.CreateTableRequest req) {
        guard.requireCap(Caps.CREATE_TABLE);
        String sql = buildCreateSql(req);
        return execute("CREATE_TABLE", req.getSchema(), req.getTable(), sql);
    }

    private String buildCreateSql(DdlDtos.CreateTableRequest req) {
        guard.requireCap(Caps.CREATE_TABLE);
        guard.requireDdlSchema(req.getSchema());
        String schema = SqlIdentifier.require(req.getSchema(), "schema");
        String table = SqlIdentifier.require(req.getTable(), "表名");
        if (metaService.exists(schema, table)) {
            throw BizException.badRequest("表已存在：" + schema + "." + table);
        }
        if (req.getColumns() == null || req.getColumns().isEmpty()) {
            throw BizException.badRequest("至少需要一个字段");
        }

        List<String> clauses = new ArrayList<>();
        List<String> constraints = new ArrayList<>();
        Set<String> names = new HashSet<>();
        List<String> primaryKeys = new ArrayList<>();
        int autoIncrementCount = 0;

        for (DdlDtos.ColumnDef column : req.getColumns()) {
            String name = SqlIdentifier.require(column.getName(), "字段名");
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw BizException.badRequest("字段名重复：" + name);
            }
            if (Boolean.TRUE.equals(column.getAutoIncrement())) {
                autoIncrementCount++;
                if (!Boolean.TRUE.equals(column.getPrimaryKey())) {
                    throw BizException.badRequest("自增字段 " + name + " 必须同时设为主键");
                }
            }
            if (Boolean.TRUE.equals(column.getPrimaryKey())) {
                primaryKeys.add(name);
            }
            clauses.add(columnClause(column, false));
            if (Boolean.TRUE.equals(column.getUnique()) && !Boolean.TRUE.equals(column.getPrimaryKey())) {
                constraints.add("UNIQUE KEY " + SqlIdentifier.quote(uniqueName(name)) + " (" + SqlIdentifier.quote(name) + ")");
            }
        }
        if (autoIncrementCount > 1) {
            throw BizException.badRequest("只能有一个自增字段");
        }
        if (!primaryKeys.isEmpty()) {
            constraints.add(0, "PRIMARY KEY (" + joinQuoted(primaryKeys) + ")");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(SqlIdentifier.qualified(schema, table)).append(" (\n  ");
        sql.append(String.join(",\n  ", clauses));
        if (!constraints.isEmpty()) {
            sql.append(",\n  ").append(String.join(",\n  ", constraints));
        }
        sql.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci");
        if (req.getComment() != null && !req.getComment().isBlank()) {
            sql.append(" COMMENT=").append(SqlIdentifier.literal(req.getComment()));
        }
        return sql.toString();
    }

    // ---------------------------------------------------------------- 删表

    public DdlDtos.DdlResult previewDropTable(DdlDtos.DropTableRequest req) {
        guard.requireCap(Caps.DROP_TABLE);
        guard.requireDdlSchema(req.getSchema());
        String schema = SqlIdentifier.require(req.getSchema(), "schema");
        String table = SqlIdentifier.require(req.getTable(), "表名");
        metaService.ensureExists(schema, table);
        DdlDtos.DdlResult result = new DdlDtos.DdlResult();
        result.setSql("DROP TABLE " + SqlIdentifier.qualified(schema, table));
        result.setMessage("该操作不可恢复");
        return result;
    }

    public DdlDtos.DdlResult dropTable(DdlDtos.DropTableRequest req) {
        guard.requireCap(Caps.DROP_TABLE);
        if (!req.getTable().equals(req.getConfirmName())) {
            throw BizException.badRequest("表名确认不一致，请重新输入");
        }
        DdlDtos.DdlResult preview = previewDropTable(req);
        return execute("DROP_TABLE", req.getSchema(), req.getTable(), preview.getSql());
    }

    // ---------------------------------------------------------------- 加字段

    public DdlDtos.DdlResult previewAddColumn(DdlDtos.AddColumnRequest req) {
        DdlDtos.DdlResult result = new DdlDtos.DdlResult();
        result.setSql(buildAddColumnSql(req));
        result.setMessage("预览：确认无误后点击执行");
        return result;
    }

    public DdlDtos.DdlResult addColumn(DdlDtos.AddColumnRequest req) {
        guard.requireCap(Caps.ALTER_COLUMN);
        String sql = buildAddColumnSql(req);
        return execute("ADD_COLUMN", req.getSchema(), req.getTable(), sql);
    }

    private String buildAddColumnSql(DdlDtos.AddColumnRequest req) {
        guard.requireCap(Caps.ALTER_COLUMN);
        guard.requireDdlSchema(req.getSchema());
        String schema = SqlIdentifier.require(req.getSchema(), "schema");
        String table = SqlIdentifier.require(req.getTable(), "表名");
        metaService.ensureExists(schema, table);
        if (req.getColumn() == null) {
            throw BizException.badRequest("字段信息不能为空");
        }
        if (Boolean.TRUE.equals(req.getColumn().getAutoIncrement())) {
            throw BizException.badRequest("新增字段暂不支持自增");
        }
        String clause = columnClause(req.getColumn(), true);
        String position = "";
        if ("FIRST".equalsIgnoreCase(req.getPosition())) {
            position = " FIRST";
        } else if ("AFTER".equalsIgnoreCase(req.getPosition())) {
            String after = SqlIdentifier.require(req.getAfterColumn(), "参考字段");
            ensureColumnExists(schema, table, after);
            position = " AFTER " + SqlIdentifier.quote(after);
        }
        return "ALTER TABLE " + SqlIdentifier.qualified(schema, table) + " ADD COLUMN " + clause + position;
    }

    // ---------------------------------------------------------------- 删字段

    public DdlDtos.DdlResult previewDropColumn(DdlDtos.DropColumnRequest req) {
        guard.requireCap(Caps.ALTER_COLUMN);
        guard.requireDdlSchema(req.getSchema());
        String schema = SqlIdentifier.require(req.getSchema(), "schema");
        String table = SqlIdentifier.require(req.getTable(), "表名");
        metaService.ensureExists(schema, table);
        String column = SqlIdentifier.require(req.getColumn(), "字段名");
        ensureColumnExists(schema, table, column);
        DdlDtos.DdlResult result = new DdlDtos.DdlResult();
        result.setSql("ALTER TABLE " + SqlIdentifier.qualified(schema, table) + " DROP COLUMN " + SqlIdentifier.quote(column));
        result.setMessage("该操作不可恢复");
        return result;
    }

    public DdlDtos.DdlResult dropColumn(DdlDtos.DropColumnRequest req) {
        guard.requireCap(Caps.ALTER_COLUMN);
        DdlDtos.DdlResult preview = previewDropColumn(req);
        return execute("DROP_COLUMN", req.getSchema(), req.getTable(), preview.getSql());
    }

    // ---------------------------------------------------------------- 索引

    public DdlDtos.DdlResult previewAddIndex(DdlDtos.AddIndexRequest req) {
        DdlDtos.DdlResult result = new DdlDtos.DdlResult();
        result.setSql(buildAddIndexSql(req));
        result.setMessage("预览：确认无误后点击执行");
        return result;
    }

    public DdlDtos.DdlResult addIndex(DdlDtos.AddIndexRequest req) {
        guard.requireCap(Caps.INDEX);
        String sql = buildAddIndexSql(req);
        return execute("ADD_INDEX", req.getSchema(), req.getTable(), sql);
    }

    private String buildAddIndexSql(DdlDtos.AddIndexRequest req) {
        guard.requireCap(Caps.INDEX);
        guard.requireDdlSchema(req.getSchema());
        String schema = SqlIdentifier.require(req.getSchema(), "schema");
        String table = SqlIdentifier.require(req.getTable(), "表名");
        metaService.ensureExists(schema, table);
        if (req.getColumns() == null || req.getColumns().isEmpty()) {
            throw BizException.badRequest("请选择要建索引的字段");
        }
        List<String> columns = new ArrayList<>();
        for (String column : req.getColumns()) {
            String name = SqlIdentifier.require(column, "字段名");
            ensureColumnExists(schema, table, name);
            columns.add(name);
        }
        String indexName = req.getName() == null || req.getName().isBlank()
                ? ("idx_" + String.join("_", columns))
                : SqlIdentifier.require(req.getName(), "索引名");
        if (indexName.length() > 64) {
            throw BizException.badRequest("索引名过长，最多 64 个字符");
        }
        boolean unique = Boolean.TRUE.equals(req.getUnique());
        return "ALTER TABLE " + SqlIdentifier.qualified(schema, table)
                + " ADD " + (unique ? "UNIQUE " : "") + "INDEX "
                + SqlIdentifier.quote(indexName) + " (" + joinQuoted(columns) + ")";
    }

    public DdlDtos.DdlResult previewDropIndex(DdlDtos.DropIndexRequest req) {
        guard.requireCap(Caps.INDEX);
        guard.requireDdlSchema(req.getSchema());
        String schema = SqlIdentifier.require(req.getSchema(), "schema");
        String table = SqlIdentifier.require(req.getTable(), "表名");
        metaService.ensureExists(schema, table);
        String indexName = SqlIdentifier.require(req.getIndexName(), "索引名");
        DdlDtos.DdlResult result = new DdlDtos.DdlResult();
        result.setSql("ALTER TABLE " + SqlIdentifier.qualified(schema, table) + " DROP INDEX " + SqlIdentifier.quote(indexName));
        result.setMessage("该操作不可恢复");
        return result;
    }

    public DdlDtos.DdlResult dropIndex(DdlDtos.DropIndexRequest req) {
        guard.requireCap(Caps.INDEX);
        DdlDtos.DdlResult preview = previewDropIndex(req);
        return execute("DROP_INDEX", req.getSchema(), req.getTable(), preview.getSql());
    }

    // ---------------------------------------------------------------- 内部方法

    private String columnClause(DdlDtos.ColumnDef column, boolean adding) {
        String type = canonicalType(column);
        StringBuilder sb = new StringBuilder();
        sb.append(SqlIdentifier.quote(column.getName())).append(" ").append(type);
        if (Boolean.TRUE.equals(column.getUnsigned()) && NUMERIC_TYPES.contains(baseType(column))) {
            sb.append(" unsigned");
        }
        boolean nullable = !Boolean.FALSE.equals(column.getNullable());
        boolean primary = Boolean.TRUE.equals(column.getPrimaryKey());
        if (primary || !nullable) {
            sb.append(" NOT NULL");
        } else {
            sb.append(" NULL");
        }
        String defaultClause = defaultClause(column);
        if (!defaultClause.isEmpty()) {
            sb.append(" ").append(defaultClause);
        }
        if (Boolean.TRUE.equals(column.getAutoIncrement())) {
            sb.append(" AUTO_INCREMENT");
        }
        if (column.getComment() != null && !column.getComment().isBlank()) {
            sb.append(" COMMENT ").append(SqlIdentifier.literal(column.getComment()));
        }
        return sb.toString();
    }

    private String defaultClause(DdlDtos.ColumnDef column) {
        String defaultType = column.getDefaultType() == null ? "NONE" : column.getDefaultType().toUpperCase(Locale.ROOT);
        boolean nullable = !Boolean.FALSE.equals(column.getNullable());
        switch (defaultType) {
            case "NONE":
                return "";
            case "NULL":
                if (!nullable) {
                    throw BizException.badRequest("字段 " + column.getName() + " 不允许为空，不能设默认 NULL");
                }
                return "DEFAULT NULL";
            case "CURRENT_TIMESTAMP":
                if (!TIME_DEFAULT_TYPES.contains(baseType(column))) {
                    throw BizException.badRequest("字段 " + column.getName() + " 只有 datetime/timestamp 才能用 CURRENT_TIMESTAMP 默认值");
                }
                return "DEFAULT CURRENT_TIMESTAMP";
            case "LITERAL":
                if (NO_LITERAL_DEFAULT.contains(baseType(column))) {
                    throw BizException.badRequest("字段 " + column.getName() + " 的类型不支持字面量默认值");
                }
                String value = column.getDefaultValue() == null ? "" : column.getDefaultValue();
                return "DEFAULT " + SqlIdentifier.literal(value);
            default:
                throw BizException.badRequest("未知的默认值类型：" + defaultType);
        }
    }

    private String canonicalType(DdlDtos.ColumnDef column) {
        String raw = column.getType().trim().toLowerCase(Locale.ROOT);
        int length = column.getLength() == null ? 0 : column.getLength();
        if ("integer".equals(raw)) {
            raw = "int";
        }
        if ("numeric".equals(raw)) {
            raw = "decimal";
        }
        switch (raw) {
            case "varchar":
                if (length < 1 || length > 16383) {
                    throw BizException.badRequest("varchar 长度需在 1-16383 之间");
                }
                return "varchar(" + length + ")";
            case "char":
                if (length < 1 || length > 255) {
                    throw BizException.badRequest("char 长度需在 1-255 之间");
                }
                return "char(" + length + ")";
            case "decimal": {
                int precision = column.getPrecision() == null ? 10 : column.getPrecision();
                int scale = column.getScale() == null ? 0 : column.getScale();
                if (precision < 1 || precision > 65) {
                    throw BizException.badRequest("decimal 精度需在 1-65 之间");
                }
                if (scale < 0 || scale > precision) {
                    throw BizException.badRequest("decimal 小数位需在 0-精度 之间");
                }
                return "decimal(" + precision + "," + scale + ")";
            }
            case "tinyint":
            case "smallint":
            case "mediumint":
            case "int":
            case "bigint":
            case "float":
            case "double":
            case "date":
            case "datetime":
            case "timestamp":
            case "time":
            case "year":
            case "tinytext":
            case "text":
            case "mediumtext":
            case "longtext":
            case "json":
                return raw;
            case "boolean":
            case "bool":
                return "tinyint(1)";
            default:
                throw BizException.badRequest("不支持的字段类型：" + raw);
        }
    }

    private String baseType(DdlDtos.ColumnDef column) {
        String type = canonicalType(column);
        int idx = type.indexOf('(');
        return idx > 0 ? type.substring(0, idx) : type;
    }

    private void ensureColumnExists(String schema, String table, String column) {
        boolean exists = metaService.listColumns(schema, table).stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(column));
        if (!exists) {
            throw BizException.badRequest("字段不存在：" + column);
        }
    }

    private String uniqueName(String column) {
        String name = "uk_" + column;
        return name.length() > 64 ? name.substring(0, 64) : name;
    }

    private String joinQuoted(List<String> names) {
        List<String> quoted = new ArrayList<>();
        for (String name : names) {
            quoted.add(SqlIdentifier.quote(name));
        }
        return String.join(", ", quoted);
    }

    private DdlDtos.DdlResult execute(String action, String schema, String table, String sql) {
        long start = System.currentTimeMillis();
        try {
            ddlJdbc.execute(sql);
            long elapsed = System.currentTimeMillis() - start;
            auditService.log(action, schema, table, sql, true, "执行成功", elapsed);
            DdlDtos.DdlResult result = new DdlDtos.DdlResult();
            result.setSql(sql);
            result.setMessage("执行成功，耗时 " + elapsed + " ms");
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            auditService.log(action, schema, table, sql, false, e.getMessage(), elapsed);
            throw new BizException(400, "DDL 执行失败：" + e.getMessage());
        }
    }
}
