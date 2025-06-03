package com.cho.polio.presentation.security.authroization;

import com.cho.polio.application.keycloak.dto.RoleRule;
import com.cho.polio.infrastructure.keycloak.dto.PermissionRule;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
public class PermissionRuleAuthorizationManager {

    public AuthorizationDecision check(Supplier<Authentication> authentication, PermissionRule permissionRule) {
        List<RoleRule> roleRules = permissionRule.getRoleRules();
        List<String> authorities = authentication.get().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        boolean hasRequiredRole = roleRules.stream()
                .anyMatch(roleRule -> authorities.contains("ROLE_" + roleRule.getName()));

        Set<String> userScopes = authorities.stream()
                .filter(auth -> auth.startsWith("SCOPE_"))
                .map(auth -> auth.substring("SCOPE_".length()))
                .collect(Collectors.toSet());

        boolean isValidScope = isValidScope(permissionRule, userScopes, authentication.get().getAuthorities());

        boolean allowed = hasRequiredRole && isValidScope;
        return new AuthorizationDecision(allowed);
    }

    private boolean isValidScope(PermissionRule permissionRule, Set<String> userScopes, Collection<? extends GrantedAuthority> authorities) {
        // 기존 isValidScope 로직을 여기서 구현하거나, 별도 클래스로 위임 가능
        // ...
        return true; // 임시
    }
}

