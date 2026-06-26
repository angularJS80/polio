package com.cho.polio.presentation.tenant.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Hibernate Multi-tenancy에서 현재 요청의 테넌트 식별자를 제공한다.
 * TenantContext에 설정된 tenantId를 반환하며, 설정되지 않은 경우 "master"를 기본값으로 사용한다.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.getTenantId();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}