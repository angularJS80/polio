package com.cho.polio.presentation.tenant.service;

import com.cho.polio.presentation.tenant.dto.ProvisionTenantRequest;
import com.cho.polio.presentation.tenant.dto.TenantDto;
import com.cho.polio.presentation.tenant.repository.TenantRepository;
import com.cho.polio.presentation.tenant.entity.Tenant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;

    /**
     * 🌟 [해결 포인트] REQUIRES_NEW 설정
     * 부모 트랜잭션이 무엇이든 간에 이를 잠시 보류(Suspend)하고,
     * 현재 바뀐 TenantContext(MASTER)를 기반으로 완전히 새로운 물리 커넥션과 트랜잭션을 엽니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Tenant saveTenant(ProvisionTenantRequest request) {
        log.info("[Transaction] Saving new tenant registry to master database...");
        Tenant saved = tenantRepository.save(Tenant.of(request));

        // 데이터베이스에 즉시 동기화하여 완벽히 커밋되도록 보장
        tenantRepository.flush();
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean existsById(String tenantId) {
        return tenantRepository.findById(tenantId).isPresent();
    }

    @Transactional(readOnly = true)
    public Optional<TenantDto> findById(String tenantId) {
        return tenantRepository.findById(tenantId).map(TenantDto::of);
    }

    @Transactional(readOnly = true)
    public Optional<TenantDto> findByTenantIdAndIsActiveTrue(String tenantId) {
        return tenantRepository.findByTenantIdAndIsActiveTrue(tenantId)
                .map(TenantDto::of);
    }

    @Transactional(readOnly = true)
    public List<TenantDto> findAllByIsActiveTrue() {
        return tenantRepository.findAllByIsActiveTrue()
                .stream()
                .map(TenantDto::of)
                .collect(Collectors.toList());
    }
}