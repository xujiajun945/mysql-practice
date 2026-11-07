package com.sqlpractice.service;

import com.sqlpractice.common.BizException;
import com.sqlpractice.common.Caps;
import com.sqlpractice.dto.MetaDtos;
import com.sqlpractice.security.PermissionGuard;
import com.sqlpractice.util.SqlIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetaService {

    private static final String SCHEMA_SQL = """
            select schema_name as name, default_character_set_name as charset
            from information_schema.schemata
            where schema_name not in ('information_schema', 'mysql', 'performance_schema', 'sys')
            order by schema_name
            """;

    private static final String TABLE_SQL = """
            select table_name as name,
                   if(table_type = 'VIEW', 'VIEW', 'BASE TABLE') as type,
                   table_rows as row_count,
                   round((data_length + index_length) / 1024, 1) as size_kb,
                   table_comment as comment
            from information_schema.tables
            where table_schema = ?
            order by table_name
            """;

    @Qualifier("roJdbcTemplate")
    private final JdbcTemplate roJdbc;
    private final PermissionGuard guard;

    public MetaService(@Qualifier("roJdbcTemplate") JdbcTemplate roJdbc, PermissionGuard guard) {
        this.roJdbc = roJdbc;
        this.guard = guard;
    }

    public List<MetaDtos.SchemaInfo> listSchemas() {
        guard.requireCap(Caps.QUERY);
        List<MetaDtos.SchemaInfo> all = roJdbc.query(SCHEMA_SQL, (rs, i) -> {
            MetaDtos.SchemaInfo info = new MetaDtos.SchemaInfo();
            info.setName(rs.getString("name"));
            info.setCharset(rs.getString("charset"));
            return info;
        });
        List<MetaDtos.SchemaInfo> visible = new ArrayList<>();
        for (MetaDtos.SchemaInfo info : all) {
            if (guard.current().canSeeSchema(info.getName())) {
                visible.add(info);
            }
        }
        return visible;
    }

    public List<MetaDtos.TableInfo> listTables(String schema) {
        guard.requireCap(Caps.QUERY);
        guard.requireSchema(schema);
        SqlIdentifier.require(schema, "schema");
        return roJdbc.query(TABLE_SQL, (rs, i) -> {
            MetaDtos.TableInfo info = new MetaDtos.TableInfo();
            info.setName(rs.getString("name"));
            info.setType(rs.getString("type"));
            info.setRowCount(rs.getLong("row_count"));
            info.setSizeKb(rs.getDouble("size_kb"));
            info.setComment(rs.getString("comment"));
            return info;
        }, schema);
    }

    public MetaDtos.TableDetail detail(String schema, String table) {
        guard.requireCap(Caps.QUERY);
        guard.requireSchema(schema);
        String s = SqlIdentifier.require(schema, "schema");
        String t = SqlIdentifier.require(table, "表名");
        ensureExists(s, t);

        MetaDtos.TableDetail detail = new MetaDtos.TableDetail();
        detail.setSchema(s);
        detail.setTable(t);
        detail.setColumns(listColumns(s, t));
        detail.setIndexes(listIndexes(s, t));
        detail.setCreateSql(showCreateTable(s, t));

        List<MetaDtos.TableInfo> tables = listTables(s);
        for (MetaDtos.TableInfo info : tables) {
            if (info.getName().equalsIgnoreCase(t)) {
                detail.setRowCount(info.getRowCount());
                detail.setSizeKb(info.getSizeKb());
                detail.setComment(info.getComment());
                break;
            }
        }
        return detail;
    }

    public MetaDtos.PreviewResult preview(String schema, String table, int limit) {
        guard.requireCap(Caps.QUERY);
        guard.requireSchema(schema);
        String s = SqlIdentifier.require(schema, "schema");
        String t = SqlIdentifier.require(table, "表名");
        ensureExists(s, t);
        int size = Math.max(1, Math.min(limit, 100));
        long start = System.currentTimeMillis();
        List<String> columns = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        roJdbc.query("select * from " + SqlIdentifier.qualified(s, t) + " limit " + size, (rs, rowNum) -> {
            if (columns.isEmpty()) {
                java.sql.ResultSetMetaData md = rs.getMetaData();
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    columns.add(md.getColumnLabel(i));
                }
            }
            List<String> row = new ArrayList<>();
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                row.add(com.sqlpractice.util.ResultSetUtil.cell(rs.getObject(i)));
            }
            rows.add(row);
            return null;
        });
        MetaDtos.PreviewResult result = new MetaDtos.PreviewResult();
        result.setColumns(columns);
        result.setRows(rows);
        result.setRowCount(rows.size());
        result.setTruncated(rows.size() >= size);
        result.setElapsedMs(System.currentTimeMillis() - start);
        return result;
    }

    public List<MetaDtos.ColumnInfo> listColumns(String schema, String table) {
        String sql = "SHOW FULL COLUMNS FROM " + SqlIdentifier.qualified(schema, table);
        return roJdbc.query(sql, (rs, i) -> {
            MetaDtos.ColumnInfo info = new MetaDtos.ColumnInfo();
            info.setName(rs.getString("Field"));
            info.setType(rs.getString("Type"));
            info.setCollation(rs.getString("Collation"));
            info.setNullable(rs.getString("Null"));
            info.setKey(rs.getString("Key"));
            info.setDefaultValue(rs.getString("Default"));
            info.setExtra(rs.getString("Extra"));
            info.setComment(rs.getString("Comment"));
            return info;
        });
    }

    public List<MetaDtos.IndexInfo> listIndexes(String schema, String table) {
        String sql = "SHOW INDEX FROM " + SqlIdentifier.qualified(schema, table);
        Map<String, MetaDtos.IndexInfo> map = new LinkedHashMap<>();
        roJdbc.query(sql, (rs, rowNum) -> {
            String name = rs.getString("Key_name");
            int nonUnique = rs.getInt("Non_unique");
            String columnName = rs.getString("Column_name");
            String indexType = rs.getString("Index_type");
            MetaDtos.IndexInfo info = map.computeIfAbsent(name, k -> {
                MetaDtos.IndexInfo created = new MetaDtos.IndexInfo();
                created.setName(k);
                created.setUnique(nonUnique == 0);
                created.setColumns(new ArrayList<>());
                return created;
            });
            info.getColumns().add(columnName);
            info.setType(indexType);
            return null;
        });
        return new ArrayList<>(map.values());
    }

    public boolean exists(String schema, String table) {
        Integer count = roJdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = ? and table_name = ?",
                Integer.class, schema, table);
        return count != null && count > 0;
    }

    public void ensureExists(String schema, String table) {
        if (!exists(schema, table)) {
            throw BizException.notFound("表不存在：" + schema + "." + table);
        }
    }

    private String showCreateTable(String schema, String table) {
        return roJdbc.queryForObject("SHOW CREATE TABLE " + SqlIdentifier.qualified(schema, table),
                (rs, i) -> rs.getString(2));
    }
}
