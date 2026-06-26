package com.cho.polio.presentation.tenant.repository;

import com.cho.polio.presentation.tenant.entity.Tenant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {

    Optional<Tenant> findByTenantIdAndIsActiveTrue(String tenantId);

    List<Tenant> findAllByIsActiveTrue();
}
