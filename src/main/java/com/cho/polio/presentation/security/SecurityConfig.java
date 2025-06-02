package com.cho.polio.presentation.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakAdminClient adminClient;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        String token = adminClient.obtainAdminToken();
        Map<String, List<String>> uriToRoles = adminClient.buildUriRoleMap(token);

        http
                .csrf(csrf -> csrf.disable()) // REST API인 경우 비활성화 권장
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authz -> {
                    uriToRoles.forEach((uri, roles) -> {
                        for (String role : roles) {
                            authz.requestMatchers(uri).hasRole(role);
                        }
                    });
                    authz.anyRequest().authenticated();
                })

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
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
}
