package com.sqlpractice.controller;

import com.sqlpractice.common.ApiResult;
import com.sqlpractice.common.Caps;
import com.sqlpractice.entity.AuditLog;
import com.sqlpractice.mapper.AuditMapper;
import com.sqlpractice.security.PermissionGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuditMapper auditMapper;
    private final PermissionGuard guard;

    @GetMapping
    public ApiResult<Map<String, Object>> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        guard.requireCap(Caps.AUDIT_VIEW);
        int pageSize = Math.max(1, Math.min(size, 100));
        int offset = Math.max(0, (Math.max(page, 1) - 1) * pageSize);
        List<AuditLog> rows = auditMapper.list(offset, pageSize);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", auditMapper.count());
        result.put("rows", rows);
        result.put("page", page);
        result.put("size", pageSize);
        return ApiResult.ok(result);
    }
}
