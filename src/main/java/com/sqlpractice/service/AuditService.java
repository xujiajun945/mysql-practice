package com.sqlpractice.service;

import com.sqlpractice.entity.AuditLog;
import com.sqlpractice.mapper.AuditMapper;
import com.sqlpractice.security.CurrentUser;
import com.sqlpractice.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditMapper auditMapper;

    public void log(String action, String schema, String table, String sql,
                    boolean success, String message, long elapsedMs) {
        AuditLog log = new AuditLog();
        CurrentUser user = UserContext.get();
        if (user != null) {
            log.setUserId(user.getId());
            log.setUsername(user.getUsername());
            log.setIp(user.getIp());
        }
        log.setAction(action);
        log.setSchemaName(schema);
        log.setTableName(table);
        log.setSqlText(truncate(sql, 5000));
        log.setSuccess(success ? 1 : 0);
        log.setMessage(truncate(message, 500));
        log.setElapsedMs((int) elapsedMs);
        auditMapper.insert(log);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
