package com.cho.polio.presentation.security.util;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

public class JwtUtil {
    public static Collection<GrantedAuthority> convertAuthorities(Jwt jwt, String clientId) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // realm_access.roles
        List<String> realmRoles = Optional.ofNullable(jwt.getClaimAsMap("realm_access"))
                .map(realm -> (List<String>) realm.get("roles"))
                .orElse(Collections.emptyList());
        realmRoles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));

        // resource_access.{client}.roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null && resourceAccess.containsKey(clientId)) {
            Map<String, Object> clientRolesMap = (Map<String, Object>) resourceAccess.get(clientId);
            List<String> clientRoles = (List<String>) clientRolesMap.get("roles");
            clientRoles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }

        // scope
        List<String> scopes = Optional.ofNullable(jwt.getClaimAsStringList("scope")).orElse(Collections.emptyList());
        scopes.forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));

        return authorities;
    }
}
