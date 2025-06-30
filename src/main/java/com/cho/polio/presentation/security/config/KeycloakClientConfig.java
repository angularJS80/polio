package com.cho.polio.presentation.security.config;

import com.polio.poliokeycloak.keycloak.client.KeycloakAdminClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakClientConfig {

    @Autowired
    private KeycloakAdminClient keycloakAdminClient;

    @PostConstruct
    public void init() {
        keycloakAdminClient.initialize(); // 안전하게 호출
    }
}
