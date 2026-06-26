package com.cho.polio.presentation.tenant.config;

import static com.cho.polio.presentation.tenant.config.TenantContext.MASTER_TENANT_ID;
import static com.cho.polio.presentation.tenant.config.TenantContext.SHARED_TENANT_ID;

import com.cho.polio.presentation.tenant.dto.TenantDto;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class LazyMultiTenantConnectionProvider
        extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String> {


    // 🌟 변경 포인트 ①: 이제 이 캐시는 tenantId가 아니라 '물리 DB URL(서버 주소)'을 키로 삼습니다!
    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    private final TenantMetadataCache tenantMetadataCache;

    public LazyMultiTenantConnectionProvider(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("sharedDataSource") DataSource sharedDataSource,
            TenantMetadataCache tenantMetadataCache) {
        this.tenantMetadataCache = tenantMetadataCache;
        this.dataSourceCache.put(MASTER_TENANT_ID, masterDataSource);
        this.dataSourceCache.put(SHARED_TENANT_ID, sharedDataSource); // 👈 이걸 넣어줘야 에러가 안 납니다!
        log.info("Master DataSource가 초기화되었습니다.");
    }

    @Override
    protected DataSource selectAnyDataSource() {
        return getDataSource(MASTER_TENANT_ID);
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        if (MASTER_TENANT_ID.equals(tenantIdentifier)) {
            return getDataSource(MASTER_TENANT_ID);
        }
        // 🌟 변경 포인트 ②: 여기서 바로 테넌트 데이터소스를 가져오지 않고, 2단계 하이브리드 로직을 수행합니다.
        return getTenantDataSource(tenantIdentifier);
    }

    private DataSource getDataSource(String tenantId) {
        DataSource dataSource = dataSourceCache.get(tenantId);
        if (dataSource == null) {
            throw new IllegalStateException("Master DataSource가 초기화되지 않았습니다. tenantId=" + tenantId);
        }
        return dataSource;
    }

    /**
     * 🌟 변경 포인트 ③: [핵심 2단계 라우팅 진입점]
     * 1단계로 URL 기준 물리 풀을 조회/생성하고,
     * 2단계로 스키마를 강제 스위칭하는 SchemaRoutingDataSource Wrapper로 감싸 반환합니다.
     */
    private DataSource getTenantDataSource(String tenantId) {
        TenantDto tenantDto = Optional.ofNullable(tenantMetadataCache.getMetadata(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("활성화된 테넌트 정보가 없습니다: " + tenantId));

        String dbUrl = tenantDto.getUrl();
        DataSource physicalPool = dataSourceCache.computeIfAbsent(dbUrl, url -> createPhysicalPool(tenantDto));

        return new SchemaRoutingDataSource(physicalPool, tenantDto.getSchemaName());
    }

    /**
     * 🌟 변경 포인트 ④: 메서드명을 역할에 맞게 createPhysicalPool로 변경하고
     * 풀 이름 생성을 URL 기준으로 단순화했습니다.
     */
    private DataSource createPhysicalPool(TenantDto tenantDto) {
        log.info("Attempting to lazy-load PHYSICAL DataSource Pool for URL: {}", tenantDto.getUrl());

        HikariDataSource dataSource = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .driverClassName(tenantDto.getDriverClassName())
                .url(tenantDto.getUrl()) // 특정 스키마명이 아닌, 물리 DB 서버 주소가 기준이 됩니다.
                .username(tenantDto.getUsername())
                .password(tenantDto.getPassword())
                .build();

        Optional.ofNullable(tenantDto.getMaxPoolSize())
                .ifPresent(dataSource::setMaximumPoolSize);
        Optional.ofNullable(tenantDto.getMinIdle())
                .ifPresent(dataSource::setMinimumIdle);

        // 물리 풀 이름 지정 (어느 테넌트의 생성 요청에 의해 시작된 파이프라인인지 로그 추적용)
        dataSource.setPoolName("HikariPool-Physical-by-" + tenantDto.getTenantId());

        log.info("Physical Connection Pool 생성 완료. 호스트 주소: {}", tenantDto.getUrl());
        return dataSource;
    }

    /**
     * 🌟 변경 포인트 ⑤: 기존 테넌트 정보가 갱신되었을 때 구버전 물리 풀을 날리는 조건도
     * tenantId가 아니라 URL(Key) 기준으로 매끄럽게 수정했습니다.
     */
    public void reloadTenantResource(String tenantId) {
        if (MASTER_TENANT_ID.equals(tenantId)) return;

        log.info("[Infrastructure] 테넌트 인프라 자원 세대교체 시작 -> tenantId: {}", tenantId);

        TenantDto oldTenant = tenantMetadataCache.getMetadata(tenantId);
        TenantDto newTenant = tenantMetadataCache.reloadSingle(tenantId);

        if (oldTenant != null && newTenant != null && !oldTenant.getUrl().equals(newTenant.getUrl())) {
            if (dataSourceCache.remove(oldTenant.getUrl()) instanceof HikariDataSource hikariDs) {
                hikariDs.close();
                log.info("[Infrastructure] 물리 DB 서버 주소가 변경되어 구버전 HikariPool을 제거했습니다. URL: {}", oldTenant.getUrl());
            }
        } else {
            log.info("[Infrastructure] 물리 주소가 동일하므로 스키마 스위칭 캐시만 갱신하고 풀은 유지합니다.");
        }
    }

    // LazyMultiTenantConnectionProvider 내부
    public void reloadTenantResourceDirect(TenantDto tenantDto) {
        String tenantId = tenantDto.getTenantId();
        log.info("[Infrastructure] 신규 테넌트 자원 즉시 갱신 -> tenantId: {}", tenantId);

        // 캐시에 바로 주입
        tenantMetadataCache.putMetadataDirectly(tenantId, tenantDto);

        // 물리 풀은 어차피 유저가 첫 요청 보낼 때 computeIfAbsent로 안전하게 생성되므로 신경 쓸 필요 없음!
    }
}