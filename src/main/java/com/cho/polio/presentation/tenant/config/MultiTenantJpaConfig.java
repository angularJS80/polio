package com.cho.polio.presentation.tenant.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Master DataSource 및 Tenant DB 지정을 통합 관리하는 JPA 설정.
 */
@Slf4j
@Configuration
// 💡 순환 참조 해결을 위해 @RequiredArgsConstructor를 제거하고 필드 주입을 끊었습니다.
@EnableConfigurationProperties(JpaProperties.class)
@EnableJpaRepositories(
        basePackages = {
                "com.cho.polio"
        },
        entityManagerFactoryRef = "multiTenantEntityManagerFactory",
        transactionManagerRef = "multiTenantTransactionManager"
)
public class MultiTenantJpaConfig {

    private final JpaProperties jpaProperties;
    private final TenantIdentifierResolver tenantIdentifierResolver;

    // 생성자 주입 구조를 간소화하여 순환 참조 고리를 완전히 해제합니다.
    public MultiTenantJpaConfig(JpaProperties jpaProperties, TenantIdentifierResolver tenantIdentifierResolver) {
        this.jpaProperties = jpaProperties;
        this.tenantIdentifierResolver = tenantIdentifierResolver;
    }

    /**
     * 1. 순 순하게 yml을 읽어 아무런 의존성 없이 가장 먼저 마스터 데이터소스를 띄웁니다.
     */
    @Bean(name = "masterDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.master.hikari")
    public DataSource masterDataSource() {
        log.info("Initializing Master DataSource natively from configuration properties.");
        // 🛑 기존 DataSourceBuilder.create().build() 대신 객체를 바로 생성합니다.
        return DataSourceBuilder.create()
                .type(com.zaxxer.hikari.HikariDataSource.class)
                .build();
    }

    /**
     * 🌟 2. 추가된 포인트: 공용(Shared) 데이터소스 빈 등록
     * YML에 작성하신 spring.datasource.shared.hikari 설정을 바인딩합니다.
     */
    @Bean(name = "sharedDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.shared.hikari")
    public DataSource sharedDataSource() {
        log.info("Initializing Shared DataSource from configuration properties.");
        return DataSourceBuilder.create()
                .type(com.zaxxer.hikari.HikariDataSource.class)
                .build();
    }


    /**
     * 2. 이제 스프링이 필요한 의존성(masterDataSource, lazyConnectionProvider)을
     * 파라미터로 직접 안전하게 꽂아줍니다.
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean multiTenantEntityManagerFactory(
            DataSource sharedDataSource,
            LazyMultiTenantConnectionProvider lazyConnectionProvider) {

        log.info("Initializing Multi-Tenant EntityManagerFactory");

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(sharedDataSource);
        em.setPackagesToScan("com.cho.polio");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

// 1. 기존 jpaProperties 안전하게 로드
        Map<String, Object> props = new HashMap<>();
        if (jpaProperties.getProperties() != null) {
            props.putAll(jpaProperties.getProperties());
        }

// 1. 멀티테넌시 핵심 연결 고리 (하이버네이트 6은 빈 주입만으로도 인식하지만, 명시적 기재로 안정성 확보)
        props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, lazyConnectionProvider);
        props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);

// 2. 🌟 지연 로딩 예외(LazyInitializationException) 원천 차단
        props.put(AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true");

// 3. 구동 시점 마스터 DB 메타데이터 트랩 방지 및 DDL 자동 수행 끄기
        props.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
        props.put(AvailableSettings.HBM2DDL_AUTO, "none"); // DDL 자동 수행 끄기

        em.setJpaPropertyMap(props);

        return em;
    }

    /**
     * 3. 트랜잭션 매니저 설정
     */
    @Bean
    @Primary
    public PlatformTransactionManager multiTenantTransactionManager(
            LocalContainerEntityManagerFactoryBean multiTenantEntityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(
                multiTenantEntityManagerFactory.getObject());
        return transactionManager;
    }

}
