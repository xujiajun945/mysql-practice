package com.sqlpractice.service;

import com.sqlpractice.common.BizException;
import com.sqlpractice.common.Caps;
import com.sqlpractice.security.PermissionGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SandboxService {

    private static final List<String> TABLES = List.of(
            "departments", "employees", "dept_emp", "dept_manager", "salaries", "titles");

    private final JdbcTemplate ddlJdbc;
    private final PermissionGuard guard;
    private final AuditService auditService;

    public SandboxService(@Qualifier("ddlJdbcTemplate") JdbcTemplate ddlJdbc,
                          PermissionGuard guard,
                          AuditService auditService) {
        this.ddlJdbc = ddlJdbc;
        this.guard = guard;
        this.auditService = auditService;
    }

    public Map<String, Object> reset() {
        guard.requireCap(Caps.USER_ADMIN);
        long start = System.currentTimeMillis();
        try {
            ddlJdbc.execute("DROP DATABASE IF EXISTS employees_lab");
            ddlJdbc.execute("CREATE DATABASE employees_lab CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            for (String table : TABLES) {
                ddlJdbc.execute("CREATE TABLE employees_lab." + table + " LIKE employees." + table);
                ddlJdbc.execute("INSERT INTO employees_lab." + table + " SELECT * FROM employees." + table);
            }
            long elapsed = System.currentTimeMillis() - start;
            auditService.log("RESET_SANDBOX", "employees_lab", null, "RESET employees_lab", true, "重建成功", elapsed);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message", "employees_lab 已重建，共 " + TABLES.size() + " 张表");
            result.put("elapsedMs", elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            auditService.log("RESET_SANDBOX", "employees_lab", null, "RESET employees_lab", false, e.getMessage(), elapsed);
            throw new BizException(500, "重建沙盒失败：" + e.getMessage());
        }
    }
}
