package com.cho.polio.infrastructure.keycloak.dto;

import lombok.Data;

@Data
public class RoleConfig {
    private String id;
    private boolean required;
}
