package com.cho.polio;

import com.cho.polio.presentation.security.KeycloakAdminClient;
import com.cho.polio.presentation.security.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.cho.polio.presentation.security.KeycloakAdminClient.CLIENT_AUTH_META;

@SpringBootTest
class PolioApplicationTests {

    @Autowired
    KeycloakAdminClient keycloakAdminClient;

    @Test
    void contextLoads() {




        //List<IdentityInfo> scopeIdentiyList = keycloakAdminClient.retrieveScopeIdentityResourceId(resourceIdentityInfo.get_id());

    }

}
