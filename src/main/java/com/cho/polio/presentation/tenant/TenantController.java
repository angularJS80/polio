package com.cho.polio.presentation.tenant;

import com.cho.polio.presentation.tenant.component.TenantProvisioningComponent;
import com.cho.polio.presentation.tenant.dto.TenantDto;
import com.cho.polio.presentation.tenant.entity.Tenant;
import com.cho.polio.presentation.enums.ApiPaths;
import com.cho.polio.presentation.tenant.dto.ProvisionTenantRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tenant", description = "테넌트 프로비저닝 API")
@RestController
@RequestMapping(ApiPaths.TENANT)
@RequiredArgsConstructor
public class TenantController {

    private final TenantProvisioningComponent provisioningComponent;

    @Operation(
            summary = "신규 테넌트 프로비저닝",
            description = "새로운 테넌트(기업)를 위한 DB 스키마를 생성하고 tenant_info에 등록합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "프로비저닝 성공",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Tenant.class))
                    )
            }
    )
    @PostMapping("/provision")
    public ResponseEntity<TenantDto> provisionTenant(@RequestBody ProvisionTenantRequest request) {
        TenantDto tenantDto = provisioningComponent.provisionTenant(request);
        return ResponseEntity.ok(tenantDto);
    }

}
