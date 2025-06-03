package com.cho.polio.presentation.security;

import com.cho.polio.application.keycloak.service.KeycloakPermissionService;
import com.cho.polio.infrastructure.keycloak.prop.KeycloakSecurityProperties;
import com.cho.polio.infrastructure.keycloak.dto.PermissionRule;
import com.cho.polio.infrastructure.keycloak.dto.Resource;
import com.cho.polio.application.keycloak.dto.RoleRule;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakPermissionService keycloakPermissionService;
    private final KeycloakSecurityProperties keycloakSecurityProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // REST API인 경우 비활성화 권장
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authz -> {
                    authorizeByPermissionRules(authz, keycloakPermissionService.getPermissionRules());
                    authz.anyRequest().authenticated();
                })

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .exceptionHandling(this::configureExceptionHandling);

        return http.build();
    }

    private void configureExceptionHandling(ExceptionHandlingConfigurer<HttpSecurity> ex) {
        ex.authenticationEntryPoint((request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        }).accessDeniedHandler((request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        });
    }


    // Uri에 검사기 주입
    private void authorizeByPermissionRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz, List<PermissionRule> permissionRules) {
        keycloakPermissionService.getPermissionRules().stream()
                .flatMap(permissionRule -> permissionRule.findResource().stream().flatMap(resource -> resource.getUris().stream()
                        .map(uri -> new AbstractMap.SimpleEntry<>(uri, permissionRule))))
                .forEach(entry -> {
                    String uri = entry.getKey();
                    PermissionRule permissionRule = entry.getValue();

                    authz.requestMatchers(uri)
                            .access((authentication, context) ->
                                    evaluateAccess(authentication, permissionRule, context.getRequest().getMethod())
                            );
                });
    }

    // 권한 검사기
    private AuthorizationDecision evaluateAccess(Supplier<Authentication> authentication, PermissionRule permissionRule, String httpMethod) {
        List<RoleRule> roleRules = permissionRule.getRoleRules();

        // 현재 사용자의 권한 목록
        List<String> authorities = authentication.get().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // Role 검사
        boolean hasRequiredRole = roleRules.stream()
                .anyMatch(roleRule -> authorities.contains("ROLE_" + roleRule.getName()));

        // 사용자가 가진 스코프 목록
        Set<String> userScopes = authorities.stream()
                .filter(auth -> auth.startsWith("SCOPE_"))
                .map(auth -> auth.substring("SCOPE_".length()))
                .collect(Collectors.toSet());

        boolean isValidScope = isValidScope(permissionRule, userScopes, httpMethod);


        // 최종 결정: 역할이 있고, 스코프가 맞으며 메서드도 허용돼야 함
        boolean allowed = hasRequiredRole && isValidScope;

        return new AuthorizationDecision(allowed);
    }

    private boolean isValidScope(PermissionRule permissionRule, Set<String> userScopes, String httpMethod) {
        // 리소스에 정의된 필수 스코프 목록 생성
        List<String> requiredScopes = makeRequiredScopes(permissionRule);

        // 1) 리소스에 할당된 스코프들이 허용하는 HTTP 메서드 집합 구함
        //    요청한 httpMethod가 허용 메서드에 포함되면, 즉시 true 반환 (사용자 Scope 검사 없이 허용)
        if (getAllowedMethodsByScopes(requiredScopes).contains(httpMethod.toUpperCase())) {
            return true;
        }

        // 2) 사용자 토큰에 포함된 Scope를 정규화 (예: "user.read" -> "read")
        Set<String> normalizedUserScopes = userScopes.stream()
                .map(this::mapHttpMethodToScope)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3) 사용자 스코프와 리소스 스코프 간 교집합 여부 확인
        //    교집합이 존재하면 true 반환 (메서드 구분 없이 권한 허용)
        if (requiredScopes.stream()
                .anyMatch(normalizedUserScopes::contains)) {
            return true;
        }

        // 4) 위 조건들에 모두 해당하지 않으면 권한 거부 (false 반환)
        return false;
    }

    private List<String> makeRequiredScopes(PermissionRule permissionRule) {
        return permissionRule.findResource()
                .map(Resource::getScopes)
                .orElse(List.of())
                .stream()
                .map(scope -> scope.getName()) // 예: "read", "write"
                .toList();
    }

    private Set<String> getAllowedMethodsByScopes(List<String> scopes) {
        return scopes.stream()
                .map(this::mapHttpMethodToScope)
                .filter(Objects::nonNull)
                .flatMap(scope -> allowedMethodsForScope(scope).stream())
                .collect(Collectors.toSet());
    }

    private Set<String> allowedMethodsForScope(String mappedScope) {
        return switch (mappedScope) {
            case "read" -> Set.of("GET");
            case "write" -> Set.of("POST", "PUT", "PATCH");
            case "edit" -> Set.of("PATCH"); // 선택적 추가
            case "remove" -> Set.of("DELETE");
            default -> Set.of();
        };
    }


    private String mapHttpMethodToScope(String scope) {
        if (scope == null) return null;

        String lower = scope.toLowerCase();

        if (containsAny(lower, "read", "view", "list", "get", "fetch", "query", "search")) return "read";
        if (containsAny(lower, "write", "create", "add", "post", "register", "insert")) return "write";
        if (containsAny(lower, "edit", "modify", "update", "change", "patch")) return "edit";
        if (containsAny(lower, "remove", "delete", "destroy", "erase", "drop")) return "remove";

        return null;
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) return true;
        }
        return false;
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

            // 3. scope → SCOPE_ 권한 추가 (필요시)
            List<String> scopes = Optional.ofNullable(jwt.getClaimAsStringList("scope"))
                    .orElse(Collections.emptyList());
            scopes.forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));

            return authorities;

        });

        return converter;
    }


}
