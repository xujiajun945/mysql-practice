package com.sqlpractice.security;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class CurrentUser {
    private Long id;
    private String username;
    private String nickname;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private Set<String> capabilities;
    private Set<String> schemas;
    private String status;
    private String ip;

    public boolean has(String cap) {
        return capabilities != null && capabilities.contains(cap);
    }

    public boolean canSeeSchema(String schema) {
        return schemas != null && schemas.contains(schema);
    }
}
