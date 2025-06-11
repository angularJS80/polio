package com.cho.polio.presentation.security.authroization;

import com.polio.poliokeycloak.keycloak.helper.KeycloakHelper;
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
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class PermissionRuleUriMapper {

    private final KeycloakHelper keycloakHelper;

    public void configureAuthorization(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        keycloakHelper.hasPermissionsPatterns()
                .forEach(pettern->{
                    authz.requestMatchers(pettern)
                            .access(this::check);
                });
    }

    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        HttpMethod httpMethod = HttpMethod.valueOf(context.getRequest().getMethod());
        String targetUri = context.getRequest().getRequestURI();
        return  keycloakHelper.decide(httpMethod,authentication.get(),targetUri);
    }

    public RequestMatcher getPublicSecurityMatcher() {
        List<RequestMatcher> matchers = keycloakHelper.hasNoPermissionsPatterns()
                .stream()
                .map(uri -> (RequestMatcher) new CustomRequestMatcher(uri))
                .toList();

        return new OrRequestMatcher(matchers);
    }
}
