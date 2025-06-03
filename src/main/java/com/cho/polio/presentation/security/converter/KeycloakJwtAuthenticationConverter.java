package com.cho.polio.presentation.security.converter;

import com.cho.polio.infrastructure.keycloak.prop.KeycloakSecurityProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
        List<GrantedAuthority> authorities = new ArrayList<>();

        // realm_access.roles
        List<String> realmRoles = Optional.ofNullable(jwt.getClaimAsMap("realm_access"))
                .map(realm -> (List<String>) realm.get("roles"))
                .orElse(Collections.emptyList());
        realmRoles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));

        // resource_access.{client}.roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null && resourceAccess.containsKey(keycloakSecurityProperties.getClientId())) {
            Map<String, Object> clientRolesMap = (Map<String, Object>) resourceAccess.get(keycloakSecurityProperties.getClientId());
            List<String> clientRoles = (List<String>) clientRolesMap.get("roles");
            clientRoles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }

        // scope
        List<String> scopes = Optional.ofNullable(jwt.getClaimAsStringList("scope")).orElse(Collections.emptyList());
        scopes.forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));

        return authorities;
    }
}
