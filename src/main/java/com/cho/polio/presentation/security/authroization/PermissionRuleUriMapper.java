package com.cho.polio.presentation.security.authroization;

import com.cho.polio.application.keycloak.service.KeycloakPermissionService;
import com.cho.polio.infrastructure.keycloak.dto.PermissionRule;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;

@Component
@RequiredArgsConstructor
public class PermissionRuleUriMapper {

    private final KeycloakPermissionService keycloakPermissionService;
    private final PermissionRuleAuthorizationManager authorizationManager;

    public void configureAuthorization(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        keycloakPermissionService.getPermissionRules().stream()
                .flatMap(permissionRule -> permissionRule.findResource().stream()
                        .flatMap(resource -> resource.getUris().stream()
                                .map(uri -> new AbstractMap.SimpleEntry<>(uri, permissionRule))))
                .forEach(entry -> {
                    String uri = entry.getKey();
                    PermissionRule permissionRule = entry.getValue();
                    authz.requestMatchers(uri)
                            .access((authentication, context) -> authorizationManager.check(authentication, permissionRule));
                });
    }
}
