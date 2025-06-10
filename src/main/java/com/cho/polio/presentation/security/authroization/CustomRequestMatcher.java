package com.cho.polio.presentation.security.authroization;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;

public class CustomRequestMatcher implements RequestMatcher {

    private final String pattern;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    public CustomRequestMatcher(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        String path = request.getServletPath();  // 혹은 getRequestURI() 상황에 맞게 선택
        return pathMatcher.match(pattern, path);
    }
}
