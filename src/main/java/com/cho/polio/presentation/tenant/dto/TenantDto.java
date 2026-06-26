package com.cho.polio.presentation.tenant.dto;

import com.cho.polio.presentation.tenant.entity.Tenant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TenantDto {
    private final String tenantId;
    private final String url;
    private final String schemaName;
    private final String driverClassName;
    private final String username;
    private final String password;
    private final Integer maxPoolSize;
    private final Integer minIdle;
    private final boolean isActive;

    /**
     * 🌟 엔티티를 불변 DTO로 변환하는 정적 팩토리 메서드
     */
    public static TenantDto of(Tenant tenant) {
        if (tenant == null) {
            return null;
        }

        return TenantDto.builder()
                .tenantId(tenant.getTenantId())
                .url(tenant.getUrl())
                .schemaName(tenant.getSchemaName())
                .driverClassName(tenant.getDriverClassName())
                .username(tenant.getUsername())
                .password(tenant.getPassword())
                .maxPoolSize(tenant.getMaxPoolSize())
                .minIdle(tenant.getMinIdle())
                .isActive(tenant.isActive())
                .build();
    }

}