package com.cho.polio.presentation.tenant.component;

import com.cho.polio.presentation.tenant.config.LazyMultiTenantConnectionProvider;
import com.cho.polio.presentation.tenant.config.TenantContext;
import com.cho.polio.presentation.tenant.config.TenantMetadataCache;
import com.cho.polio.presentation.tenant.dto.ProvisionTenantRequest;
import com.cho.polio.presentation.tenant.dto.TenantDto;
import com.cho.polio.presentation.tenant.service.TenantProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.cho.polio.presentation.tenant.util.DatabaseAdminUtil;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantProvisioningComponent {

    private final TenantProvisioningService provisioningService;
    private final LazyMultiTenantConnectionProvider lazyMultiTenantConnectionProvider;
    private final TenantMetadataCache tenantMetadataCache;
    private final DatabaseAdminUtil databaseAdminUtil;

    /**
     * 신규 테넌트 인프라 프로비저닝 메인 파이프라인
     */
    public TenantDto provisionTenant(ProvisionTenantRequest request) {
        // 🌟 [중요] 스레드 테넌트 오염 방지를 위한 컨텍스트 샌드박스 구축
        String originalTenantId = TenantContext.getTenantId();

        try {
            // 마스터 레지스트리 조회를 위해 컨텍스트를 MASTER로 고정
            TenantContext.setTenantId(TenantContext.MASTER_TENANT_ID);

            // 1. 테넌트 중복 체크
            if (provisioningService.existsById(request.getTenantId())) {
                throw new IllegalStateException("Tenant \'" + request.getTenantId() + "\' already exists.");
            }

            // 2. 물리 MySQL 서버에 대상 스키마(Database) 생성
            databaseAdminUtil.createDatabase(request.getUrl(), request.getUsername(), request.getPassword(), request.getSchemaName());

            // 3. 기준(Default) 테넌트로부터 마이그레이션용 DDL 추출
            TenantDto referenceTenant = provisioningService.findById(request.getReferenceTenantId())
                    .orElseGet(() -> {
                        log.info("Reference tenant \'{}\' not found in DB. Fetching from TenantMetadataCache.", request.getReferenceTenantId());
                        TenantDto cachedTenant = tenantMetadataCache.getMetadata(request.getReferenceTenantId());
                        if (cachedTenant == null) {
                            throw new IllegalArgumentException("Reference tenant \'" + request.getReferenceTenantId() + "\' could not be found anywhere (DB & Cache).");
                        }
                        return cachedTenant;
                    });
            List<String> ddlStatements = databaseAdminUtil.extractSchemaDdl(
                    referenceTenant.getUrl(), referenceTenant.getSchemaName(),
                    referenceTenant.getUsername(), referenceTenant.getPassword());

            // 4. 새롭게 생성된 스키마에 DDL 순차 실행 (테이블 구조 생성)
            databaseAdminUtil.executeDdlOnDatabase(request.getUrl(), request.getSchemaName(), request.getUsername(), request.getPassword(), ddlStatements);

            // 5. 마스터 DB 레지스트리에 테넌트 메타데이터 영속화
            TenantDto tenantDto = TenantDto.of(provisioningService.saveTenant(request));

            // 6. 런타임 멀티테넌시 라우팅 인프라(메모리 캐시)에 즉시 동기화
            lazyMultiTenantConnectionProvider.reloadTenantResourceDirect(tenantDto);

            log.info("Successfully provisioned and saved tenant info for \'{}\'", request.getTenantId());
            return tenantDto;

        } finally {
            // 🌟 작업 완료 후 원래 요청 스레드의 테넌트 상태로 원복 (ThreadLocal 정리)
            TenantContext.setTenantId(originalTenantId);
        }
    }
}
