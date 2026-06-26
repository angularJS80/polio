package com.cho.polio.presentation.tenant.config;

import static com.cho.polio.presentation.tenant.config.TenantContext.SHARED_TENANT_ID;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 모든 HTTP 요청에 대해 JWT 토큰에서 tenantId(기업 식별자)를 추출하여
 * TenantContext에 설정하는 Servlet Filter.
 *
 * 필터 체인 최상단에 위치하여 (Order 0) Security Filter Chain보다 먼저 실행된다.
 *
 * - JWT의 claim에서 "tenant_id" 값을 추출 (Keycloak 사용자 attribute에 매핑 필요)
 * - tenant_id가 없으면 기본값 "master" 사용
 * - 요청 종료 후 반드시 TenantContext.clear() 호출하여 ThreadLocal 정리
 */
@Slf4j
@Component
@Order(0)
public class TenantFilter implements Filter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;

    public TenantFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantId = extractTenantIdFromToken(httpRequest);

        try {
            TenantContext.setTenantId(tenantId);
            log.debug("Tenant set for request [{} {}]: {}",
                    httpRequest.getMethod(), httpRequest.getRequestURI(), tenantId);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            log.debug("TenantContext cleared for request [{} {}]",
                    httpRequest.getMethod(), httpRequest.getRequestURI());
        }
    }

    /**
     * HTTP 요청의 Authorization 헤더에서 Bearer JWT를 추출하고,
     * JWT의 "tenant_id" claim에서 테넌트 ID를 읽어온다.
     */
    private String extractTenantIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return SHARED_TENANT_ID;
        }

        try {
            String token = authHeader.substring(BEARER_PREFIX.length());
            Jwt jwt = jwtDecoder.decode(token);

            // JWT claim에서 tenant_id 추출
            // Keycloak 사용자 attribute에 "tenant_id"를 매핑한 경우 사용 가능
            String tenantId = jwt.getClaimAsString("tenant");

            if (tenantId == null || tenantId.isBlank()) {
                log.debug("No tenant_id in JWT, using default: {}", SHARED_TENANT_ID);
                return SHARED_TENANT_ID;
            }

            return tenantId;
        } catch (Exception e) {
            log.warn("Failed to extract tenant from JWT: {}", e.getMessage());
            return SHARED_TENANT_ID;
        }
    }
}