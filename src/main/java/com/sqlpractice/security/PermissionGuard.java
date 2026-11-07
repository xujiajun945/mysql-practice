package com.sqlpractice.security;

import com.sqlpractice.common.BizException;
import com.sqlpractice.common.Caps;
import com.sqlpractice.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionGuard {

    private final AppProperties props;

    public CurrentUser current() {
        CurrentUser user = UserContext.get();
        if (user == null) {
            throw BizException.unauthorized("未登录");
        }
        return user;
    }

    public void requireCap(String cap) {
        if (!current().has(cap)) {
            throw BizException.forbidden("没有权限：" + cap);
        }
    }

    public boolean hasCap(String cap) {
        return current().has(cap);
    }

    /** 普通浏览：只要求 schema 在角色的白名单内 */
    public void requireSchema(String schema) {
        if (schema == null || schema.isBlank()) {
            throw BizException.badRequest("schema 不能为空");
        }
        if (!current().canSeeSchema(schema) && !current().has(Caps.USER_ADMIN)) {
            throw BizException.forbidden("没有访问 " + schema + " 的权限");
        }
    }

    /** DDL：必须落在允许写的沙盒 schema 上 */
    public void requireDdlSchema(String schema) {
        requireSchema(schema);
        if (!props.getDdlSchemas().contains(schema)) {
            throw BizException.forbidden("该 schema 不允许执行 DDL，仅 " + String.join("、", props.getDdlSchemas()) + " 可写");
        }
    }
}
