package com.cho.polio.presentation.security.authroization;

import com.polio.poliokeycloak.keycloak.service.KeycloakPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PermissionRuleUriMapper {

    private final KeycloakPermissionService keycloakPermissionService;

    public void configureAuthorization(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        keycloakPermissionService.getUris()
                .forEach(uri->{
                    if(!keycloakPermissionService.isNoPermission(uri)){
                        authz.requestMatchers(uri)
                                .access((authentication, context) -> check(authentication.get(),context,uri));
                    }
                });
    }

    public AuthorizationDecision check(Authentication authentication, RequestAuthorizationContext context, String uri) {
        HttpMethod httpMethod = HttpMethod.valueOf(context.getRequest().getMethod());
        return  new AuthorizationDecision(keycloakPermissionService.umaCheck(httpMethod,authentication,uri));
    }

    public RequestMatcher getPublicSecurityMatcher() {
        List<RequestMatcher> matchers = keycloakPermissionService.
                hasNoPermissionsResources().stream()
                .flatMap(resource -> resource.getUris().stream())
                .map(uri -> (RequestMatcher) new CustomRequestMatcher(uri))
                .toList();

        return new OrRequestMatcher(matchers);
    }
}
