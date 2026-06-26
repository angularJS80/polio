package com.cho.polio.presentation.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProvisionTenantRequest {

    @Schema(description = "테넌트 ID", example = "polio-mariadb", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantId;

    // 🌟 테넌트별로 사용할 JDBC 드라이버를 동적으로 입력받음
    @Schema(description = "테넌트 DB 드라이버 클래스명", example = "org.mariadb.jdbc.Driver", requiredMode = Schema.RequiredMode.REQUIRED)
    private String driverClassName;

    @Schema(description = "테넌트 DB 스키마명", example = "tenant_maria_db", requiredMode = Schema.RequiredMode.REQUIRED)
    private String schemaName;

    @Schema(description = "테넌트 DB 접속 URL",
            example = "jdbc:mariadb://localhost:3308/tenant_maria_db",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;

    @Schema(description = "테넌트 DB 접속 사용자명", example = "root", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "테넌트 DB 접속 비밀번호", example = "secret", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "스키마 복사에 사용할 기준 테넌트 ID", example = "polio_master", requiredMode = Schema.RequiredMode.REQUIRED)
    private String referenceTenantId;
}