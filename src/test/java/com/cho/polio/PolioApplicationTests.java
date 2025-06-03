package com.cho.polio;

import com.cho.polio.infrastructure.keycloak.client.KeycloakAdminClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PolioApplicationTests {

    @Autowired
    KeycloakAdminClient keycloakAdminClient;

    @Test
    void contextLoads() {
        //List<IdentityInfo> scopeIdentiyList = keycloakAdminClient.retrieveScopeIdentityResourceId(resourceIdentityInfo.get_id());
    }

}
