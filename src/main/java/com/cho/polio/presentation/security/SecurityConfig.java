package com.cho.polio.presentation.security;

import com.cho.polio.presentation.security.dto.PermissionRule;
import com.cho.polio.presentation.security.dto.Resource;
import com.cho.polio.presentation.security.dto.RoleRule;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.function.Supplier;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakSecurityProperties keycloakSecurityProperties;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        List<PermissionRule> permissionRules = keycloakAdminClient.getPermissionRules();

        http
                .csrf(csrf -> csrf.disable()) // REST API인 경우 비활성화 권장
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authz -> {

                    authorizeByPermissionRules(authz, keycloakAdminClient.getPermissionRules());
                    authz.anyRequest().authenticated();
                })

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt->jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                        })
                );

        return http.build();
    }

    // Uri에 검사기 주입
    private void authorizeByPermissionRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz, List<PermissionRule> permissionRules) {
        keycloakAdminClient.getPermissionRules()
            .forEach(permissionRule -> {
                permissionRule.findResource().ifPresent(resource -> {
                    resource.getUris()
                            .forEach(uri -> {
                                authz.requestMatchers(uri)
                                .access((authentication, context) ->
                                     evaluateAccess(authentication, permissionRule, context.getRequest().getMethod())
                                );
                    });
                });
            });
    }

    // 권한 검사기
    private AuthorizationDecision evaluateAccess(Supplier<Authentication> authentication, PermissionRule permissionRule, String httpMethod) {
        List<RoleRule> roleRules = permissionRule.getRoleRules();

        List<String> requiredScopes = permissionRule.findResource()
                .map(Resource::getScopes)
                .orElse(List.of())
                .stream()
                .map(scope -> "SCOPE_" + scope.getName())
                .toList();


        // 현재 사용자의 권한 목록
        List<String> authorities = authentication.get().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();


        // Role 검사
        boolean hasRequiredRole = roleRules.stream()
                .anyMatch(roleRule -> authorities.contains("ROLE_" + roleRule.getName()));

        // Scope가 설정된 경우에만 추가 검사
        String requiredScopeName = mapHttpMethodToScope(httpMethod);
        boolean hasRequiredScope = requiredScopeName == null || authorities.contains("SCOPE_" + requiredScopeName);

        // 판단: 롤은 항상 필요, 스코프는 해당 메서드에 매핑된 게 있다면 체크
        boolean allowed = hasRequiredRole && hasRequiredScope;

        return new AuthorizationDecision(allowed);

    }

    private String mapHttpMethodToScope(String method) {
        return switch (method) {
            case "GET" -> "read";
            case "POST" -> "write";
            case "PUT" -> "edit";
            case "DELETE" -> "remove";
            default -> null; // 기타 메서드는 스코프 체크 없음
        };
    }



    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();

            // 1. realm_access.roles
            List<String> realmRoles = Optional.ofNullable(jwt.getClaimAsMap("realm_access"))
                    .map(realm -> (List<String>) realm.get("roles"))
                    .orElse(Collections.emptyList());
            realmRoles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));

            // 2. resource_access.{client}.roles
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null && resourceAccess.containsKey(keycloakSecurityProperties.getClientId())) {
                Map<String, Object> clientRolesMap = (Map<String, Object>) resourceAccess.get(keycloakSecurityProperties.getClientId());
                List<String> clientRoles = (List<String>) clientRolesMap.get("roles");
                clientRoles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }

            return authorities;
        });
        return converter;
    }



}
