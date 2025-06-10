package com.cho.polio.presentation.security.converter;

import com.cho.polio.presentation.security.util.JwtUtil;
import com.polio.poliokeycloak.keycloak.client.prop.KeycloakSecurityProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class KeycloakJwtAuthenticationConverter extends JwtAuthenticationConverter {

    private final KeycloakSecurityProperties keycloakSecurityProperties;

    public KeycloakJwtAuthenticationConverter(KeycloakSecurityProperties keycloakSecurityProperties) {
        this.keycloakSecurityProperties = keycloakSecurityProperties;
        setJwtGrantedAuthoritiesConverter(this::convertAuthorities);
    }

    private Collection<GrantedAuthority> convertAuthorities(Jwt jwt) {
        return JwtUtil.convertAuthorities(jwt, keycloakSecurityProperties.getClientId());
    }
}
