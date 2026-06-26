package com.cho.polio.presentation.tenant.config;

import static com.cho.polio.presentation.tenant.config.TenantContext.MASTER_TENANT_ID;
import static com.cho.polio.presentation.tenant.config.TenantContext.SHARED_TENANT_ID;

import com.cho.polio.presentation.tenant.dto.TenantDto;
import com.cho.polio.presentation.tenant.service.TenantProvisioningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🌟 [역할 정립] 오직 Redis 등으로 손쉽게 대체 가능한 '테넌트 메타데이터'만 전담 전송/캐싱하는 레이어.
 */
@Component
@Slf4j
public class TenantMetadataCache implements ApplicationRunner {


    // 🎯 오직 순수 텍스트 정보(Tenant 객체)만 가집니다. (추후 Redis 대체 타겟)
    private final Map<String, TenantDto> metadataMap = new ConcurrentHashMap<>();

    private final ObjectProvider<TenantProvisioningService> tenantProvisioningServiceProvider;

    public TenantMetadataCache(ObjectProvider<TenantProvisioningService> tenantProvisioningServiceProvider) {
        this.tenantProvisioningServiceProvider = tenantProvisioningServiceProvider;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("[TenantCache Warm-up] 전사 테넌트 메타데이터 동기화 시작...");
        reloadAllMetadata();
    }

    /**
     * 캐시에서 메타데이터 조회 (없으면 실시간으로 가져옴)
     */
    public TenantDto getMetadata(String tenantId) {
        TenantDto tenantDto = metadataMap.get(tenantId);
        if (tenantDto == null) {
            synchronized (this) {
                tenantDto = metadataMap.get(tenantId);
                if (tenantDto == null) {
                    tenantDto = reloadSingleMetadataAndGet(tenantId);
                }
            }
        }
        return tenantDto;
    }

    /**
     * 🌟 [단건 청소] 외부 수정/제거 발생 시 메타데이터 캐시를 최신화하거나 방출합니다.
     */
    public synchronized TenantDto reloadSingle(String tenantId) {
        TenantDto updatedTenant = reloadSingleMetadataAndGet(tenantId);
        if (updatedTenant == null) {
            metadataMap.remove(tenantId);
            log.info("[Metadata Cache Evict] 비활성화된 테넌트 메타데이터 캐시 삭제 완료: {}", tenantId);
        }
        return updatedTenant;
    }

    public synchronized void reloadAllMetadata() {
        String originalTenantId = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(MASTER_TENANT_ID);

            // 1. 기존 캐시 백업 시도
            TenantDto masterBackup = metadataMap.get(MASTER_TENANT_ID);
            TenantDto sharedBackup = metadataMap.get(SHARED_TENANT_ID);

            metadataMap.clear();

            // 2. 백업본이 존재하면 복구, 없다면 기본 정적 뼈대라도 강제 생성 (방어선 구축)
            if (masterBackup != null) {
                metadataMap.put(MASTER_TENANT_ID, masterBackup);
            } else {
                metadataMap.put(MASTER_TENANT_ID, TenantDto.builder().tenantId(MASTER_TENANT_ID).schemaName("polio_master").isActive(true).build());
            }

            if (sharedBackup != null) {
                metadataMap.put(SHARED_TENANT_ID, sharedBackup);
            } else {
                metadataMap.put(SHARED_TENANT_ID, TenantDto.builder().tenantId(SHARED_TENANT_ID).schemaName("polio_shared").isActive(true).build());
            }

            // 3. 동적 병원 테넌트 로드
            tenantProvisioningServiceProvider.getObject().findAllByIsActiveTrue()
                    .forEach(tenantDto -> metadataMap.put(tenantDto.getTenantId(), tenantDto));

            log.info("[Metadata Cache] 전체 동기화 완료. 총 캐시 수: {}", metadataMap.size());
        } catch (Exception e) {
            log.error("[Metadata Cache] Warm-up 중 예외 발생", e);
        } finally {
            TenantContext.setTenantId(originalTenantId);
        }
    }

    private TenantDto reloadSingleMetadataAndGet(String tenantId) {
        String originalTenantId = TenantContext.getTenantId();
        try {
            // 1. 마스터 DB 조회를 위해 테넌트 컨텍스트 스위칭
            TenantContext.setTenantId(MASTER_TENANT_ID);

            // 2. Optional 파이프라인으로 자연스럽게 연결
            return tenantProvisioningServiceProvider.getObject()
                    .findByTenantIdAndIsActiveTrue(tenantId) // Optional<Tenant> 반환
                    .map(tenantDto -> {
                        metadataMap.put(tenantId, tenantDto); // 변환된 DTO를 로컬 캐시에 저장
                        log.info("[Metadata Cache Load] 테넌트 캐시 갱신 완료: {}", tenantId);
                        return tenantDto;                    // 다음 파이프라인으로 DTO 전달
                    })
                    .orElse(null);                           // DB에 없거나 비활성화 상태면 null 리턴

        } finally {
            // 3. 어떤 상황에서든 원래 테넌트 컨텍스트로 복구 (안전장치)
            TenantContext.setTenantId(originalTenantId);
        }
    }
    /**
     * 🌟 [새로 추가할 메서드] DB 조회 없이 생성/수정된 테넌트를 캐시에 직접 즉시 반영합니다.
     */
    public synchronized void putMetadataDirectly(String tenantId, TenantDto tenantDto) {
        if (tenantDto != null && tenantDto.isActive()) {
            metadataMap.put(tenantId, tenantDto);
            log.info("[Metadata Cache Direct] 신규 테넌트 메타데이터 즉시 캐싱 완료: {}", tenantId);
        }
    }
}