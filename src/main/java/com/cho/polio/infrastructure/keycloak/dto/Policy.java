package com.cho.polio.infrastructure.keycloak.dto;

import lombok.Data;
import java.util.Map;

@Data
public class Policy extends IdentityInfo {
    private String description;
    private String type;
    private String logic;
    private String decisionStrategy;
    private Map<String, String> config;
}