package com.cho.polio.presentation.tenant.config;

import static com.cho.polio.presentation.tenant.config.TenantContext.MASTER_TENANT_ID;
import static com.cho.polio.presentation.tenant.config.TenantContext.SHARED_TENANT_ID;

import com.cho.polio.presentation.tenant.dto.TenantDto;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DefaultDatabaseInitializer {

    private final DataSource sharedDataSource;
    private final DataSource masterDataSource;
    private final LazyMultiTenantConnectionProvider multiTenantConnectionProvider;
    private final TenantMetadataCache tenantMetadataCache;

    private final String masterDdlAuto;
    private final String sharedDdlAuto;

    // 🌟 application.yml의 프로퍼티에서 가장 확실한 원천 정보를 명확하게 불러옵니다.
    private final String masterUrl;
    private final String masterDriver;
    private final String masterUser;
    private final String masterPass;

    private final String sharedUrl;
    private final String sharedDriver;
    private final String sharedUser;
    private final String sharedPass;

    public DefaultDatabaseInitializer(
            @Qualifier("sharedDataSource") DataSource sharedDataSource,
            @Qualifier("masterDataSource") DataSource masterDataSource,
            LazyMultiTenantConnectionProvider multiTenantConnectionProvider,
            TenantMetadataCache tenantMetadataCache,

            // 프로퍼티 명시적 주입 (Master)
            @Value("${spring.datasource.master.hikari.jdbc-url}") String masterUrl,
            @Value("${spring.datasource.master.hikari.driver-class-name}") String masterDriver,
            @Value("${spring.datasource.master.hikari.username}") String masterUser,
            @Value("${spring.datasource.master.hikari.password}") String masterPass,
            @Value("${datasource.master.ddl-auto:update}") String masterDdlAuto,

            // 프로퍼티 명시적 주입 (Shared)
            @Value("${spring.datasource.shared.hikari.jdbc-url}") String sharedUrl,
            @Value("${spring.datasource.shared.hikari.driver-class-name}") String sharedDriver,
            @Value("${spring.datasource.shared.hikari.username}") String sharedUser,
            @Value("${spring.datasource.shared.hikari.password}") String sharedPass,
            @Value("${datasource.shared.ddl-auto:create}") String sharedDdlAuto) {

        this.sharedDataSource = sharedDataSource;
        this.masterDataSource = masterDataSource;
        this.multiTenantConnectionProvider = multiTenantConnectionProvider;
        this.tenantMetadataCache = tenantMetadataCache;

        this.masterUrl = masterUrl;
        this.masterDriver = masterDriver;
        this.masterUser = masterUser;
        this.masterPass = masterPass;
        this.masterDdlAuto = masterDdlAuto;

        this.sharedUrl = sharedUrl;
        this.sharedDriver = sharedDriver;
        this.sharedUser = sharedUser;
        this.sharedPass = sharedPass;
        this.sharedDdlAuto = sharedDdlAuto;
    }

    @PostConstruct
    public void initializeAllDatabases() {
        // 1. 시스템 핵심 뼈대인 두 정적 DB의 DDL 스크립트를 먼저 수행합니다.
        initializeSchema(masterDataSource, "polio_master 데이터베이스", masterDdlAuto);
        initializeSchema(sharedDataSource, "polio_shared 데이터베이스", sharedDdlAuto);

        // 2. 프로퍼티 원천 정보를 기반으로 껍데기뿐인 가짜가 아닌 진짜 TenantDto 2개를 완벽하게 조립합니다.
        TenantDto masterTenantDto = TenantDto.builder()
                .tenantId(MASTER_TENANT_ID)
                .url(masterUrl)
                .schemaName("polio_master")
                .driverClassName(masterDriver)
                .username(masterUser)
                .password(masterPass)
                .isActive(true)
                .build();

        TenantDto sharedTenantDto = TenantDto.builder()
                .tenantId(SHARED_TENANT_ID)
                .url(sharedUrl)
                .schemaName("polio_shared")
                .driverClassName(sharedDriver)
                .username(sharedUser)
                .password(sharedPass)
                .isActive(true)
                .build();

        // 3. 🎯 [가장 완벽한 타이밍] 스키마 빌드가 끝난 직후, 각 캐시 저장소에 고정 자산을 때려 넣습니다.
        // 메타데이터(DTO) 캐시에 안착
        tenantMetadataCache.putMetadataDirectly(MASTER_TENANT_ID, masterTenantDto);
        tenantMetadataCache.putMetadataDirectly(SHARED_TENANT_ID, sharedTenantDto);

        log.info("[Initialization] 프로퍼티 기반 고정 인프라(Master/Shared) 캐싱 전역 동기화 완료.");
    }

    private void initializeSchema(DataSource dataSource, String dbName, String ddlOption) {
        log.info("[Initialization] {} DDL 자동 수행 시작 (옵션: {})", dbName, ddlOption);

        if ("none".equalsIgnoreCase(ddlOption)) {
            log.info("[Initialization] {} 옵션이 \'none\'이므로 초기화를 건너뜀.", dbName);
            return;
        }

        try {
            LocalContainerEntityManagerFactoryBean rawEmf = new LocalContainerEntityManagerFactoryBean();
            rawEmf.setDataSource(dataSource);
            rawEmf.setPackagesToScan("com.cho.polio");
            rawEmf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

            Map<String, Object> props = new HashMap<>();
            props.put("hibernate.hbm2ddl.auto", ddlOption);
            props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
            props.put("hibernate.temp.use_jdbc_metadata_defaults", "false");

            rawEmf.setJpaPropertyMap(props);
            rawEmf.afterPropertiesSet();
            rawEmf.destroy();
            log.info("[Initialization] {} DDL 자동 수행 완료.", dbName);
        } catch (Exception e) {
            log.error("[Initialization] {} 초기화 중 치명적 예외 발생", dbName, e);
        }
    }
}
