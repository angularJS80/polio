package com.cho.polio.presentation.security.dto;

import lombok.Data;

@Data
public class RoleRule {
    private String id;
    private String name;
    private boolean required;

    public RoleRule(RoleConfig roleConfig, String name) {
        this.id = roleConfig.getId();
        this.name = name;
        this.required = roleConfig.isRequired();
    }

    public static RoleRule of(RoleConfig roleConfig, String name) {
        return new RoleRule(roleConfig,name);
    }
}
