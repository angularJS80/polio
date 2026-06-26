package com.cho.polio.presentation.tenant.entity;

import com.cho.polio.presentation.tenant.dto.ProvisionTenantRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 테넌트(기업)별 데이터베이스 접속 정보를 저장하는 엔티티.
 * 이 엔티티는 Master DB(레지스트리 DB)의 tenant_info 테이블에 저장된다.
 * 각 테넌트가 사용할 실제 DB 연결 정보(url, 계정 등)를 보유한다.
 */
@Entity
@Table(name = "tenant_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant {

    @Id
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "schema_name", nullable = false, length = 100)
    private String schemaName;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "driver_class_name", nullable = false, length = 200)
    private String driverClassName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "max_pool_size")
    private Integer maxPoolSize;

    @Column(name = "min_idle")
    private Integer minIdle;

    @Builder
    public Tenant(String tenantId, String schemaName, String url, String username,
                  String password, String driverClassName, boolean isActive,
                  Integer maxPoolSize, Integer minIdle) {
        this.tenantId = tenantId;
        this.schemaName = schemaName;
        this.url = url;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.isActive = isActive;
        this.maxPoolSize = maxPoolSize;
        this.minIdle = minIdle;
    }

    public static Tenant of(ProvisionTenantRequest request) {
        return Tenant.builder()
                .tenantId(request.getTenantId())
                .schemaName(request.getSchemaName())
                .url(request.getUrl())
                .username(request.getUsername())
                .password(request.getPassword())
                .driverClassName(request.getDriverClassName())
                .isActive(true)
                .build();
    }
}