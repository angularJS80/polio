package com.cho.polio.presentation.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "keycloak")
@EnableConfigurationProperties(KeycloakSecurityProperties.class)
public class KeycloakSecurityProperties {
    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String username;
    private String password;
}