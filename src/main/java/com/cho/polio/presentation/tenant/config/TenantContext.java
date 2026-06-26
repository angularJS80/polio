package com.cho.polio.presentation.tenant.config;

import lombok.extern.slf4j.Slf4j;

/**
 * 현재 요청 스레드의 테넌트 ID를 보관하는 ThreadLocal 컨텍스트.
 * TenantFilter에서 JWT 토큰으로부터 추출한 tenantId를 설정하고,
 * LazyMultiTenantConnectionProvider와 TenantIdentifierResolver에서 사용한다.
 */
@Slf4j
public class TenantContext {

    public static final String SHARED_TENANT_ID = "polio_shared";
    public static final String MASTER_TENANT_ID = "polio_master";
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        log.debug("TenantContext set: {}", tenantId);
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        String tenantId = CURRENT_TENANT.get();
        return tenantId != null ? tenantId : MASTER_TENANT_ID;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}