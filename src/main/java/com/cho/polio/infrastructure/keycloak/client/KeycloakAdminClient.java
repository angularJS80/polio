package com.cho.polio.infrastructure.keycloak.client;

import com.cho.polio.application.keycloak.dto.ClientAuthMeta;
import com.cho.polio.infrastructure.keycloak.dto.*;
import com.cho.polio.infrastructure.keycloak.prop.KeycloakSecurityProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(KeycloakSecurityProperties.class)
public class KeycloakAdminClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final KeycloakSecurityProperties props;
    private String CLIENT_UUID;
    private HttpEntity<?> HTTP_ENTITY;
    // 클라이언트 역할 ID → 역할명 캐싱
    private Map<String, String> cachedRoleIdNameMap;
    public static ClientAuthMeta CLIENT_AUTH_META;

    public String obtainAdminToken() {
        String tokenUrl = props.getServerUrl() + "/realms/" + props.getRealm() + "/protocol/openid-connect/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "password");
        params.add("client_id", props.getClientId());
        params.add("client_secret", props.getClientSecret());
        params.add("username", props.getUsername());
        params.add("password", props.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
        return (String) response.getBody().get("access_token");
    }

    private HttpEntity<?> makeAccessHeader() {
        String token = obtainAdminToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private List<Resource> retrieveAllResources() {
        String resourceUrl = String.format("%s/admin/realms/%s/clients/%s/authz/resource-server/resource",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID);

        return restTemplate.exchange(
                resourceUrl,
                HttpMethod.GET,
                HTTP_ENTITY,
                new ParameterizedTypeReference<List<Resource>>() {
                }
        ).getBody();
    }

    private List<Permission> retrieveAllPermissions() {
        String permUrl = String.format("%s/admin/realms/%s/clients/%s/authz/resource-server/permission",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID);

        return restTemplate.exchange(
                permUrl,
                HttpMethod.GET,
                HTTP_ENTITY,
                new ParameterizedTypeReference<List<Permission>>() {
                }
        ).getBody();
    }

    private List<Policy> retrieveAllPolicies() {
        String policyListUrl = String.format(
                "%s/admin/realms/%s/clients/%s/authz/resource-server/policy",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID
        );
        return restTemplate.exchange(
                policyListUrl,
                HttpMethod.GET,
                HTTP_ENTITY,
                new ParameterizedTypeReference<List<Policy>>() {
                }
        ).getBody();
    }

    private List<Role> retrieveAllRoles() {
        String rolesUrl = String.format("%s/admin/realms/%s/clients/%s/roles",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID);

        return restTemplate.exchange(
                rolesUrl,
                HttpMethod.GET,
                HTTP_ENTITY,
                new ParameterizedTypeReference<List<Role>>() {
                }
        ).getBody();
    }

    private void makeClientAuthMeta() {
        CLIENT_UUID = getClientUuid();
        CLIENT_AUTH_META = ClientAuthMeta.of(retrieveAllPermissions(),
                retrieveAllResources(),
                retrieveAllPolicies(),
                retrieveAllRoles()
        );
    }

    public List<_IdentityInfo> retrieveResourceIdentityPermission(String permissionId) {
        String resourceUrl = String.format("%s/admin/realms/%s/clients/%s/authz/resource-server/policy/%s/resources",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID, permissionId);

        return restTemplate.exchange(
                resourceUrl,
                HttpMethod.GET,
                HTTP_ENTITY,
                new ParameterizedTypeReference<List<_IdentityInfo>>() {
                }
        ).getBody();
    }

    private List<IdentityInfo> retrieveScopeIdentityResourceId(String resourceId) {
        String resourceUrl = String.format("%s/admin/realms/%s/clients/%s/authz/resource-server/resource/%s/scopes",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID, resourceId);

        return restTemplate.exchange(
                resourceUrl,
                HttpMethod.GET,
                HTTP_ENTITY,
                new ParameterizedTypeReference<List<IdentityInfo>>() {
                }
        ).getBody();
    }

    public List<Policy> retrievePolicyPermissionId(String permissionId) {
        String resourceUrl = String.format("%s/admin/realms/%s/clients/%s/authz/resource-server/policy/%s/associatedPolicies",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID, permissionId);

        return restTemplate.exchange(
                resourceUrl,
                HttpMethod.GET,
                HTTP_ENTITY,
                new ParameterizedTypeReference<List<Policy>>() {
                }
        ).getBody();
    }

    public Optional<PolicyWithRole> findPolicyWithRoleByPolicyId(String policyId) {
        String resourceUrl = String.format("%s/admin/realms/%s/clients/%s/authz/resource-server/policy/role/%s",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID, policyId);

        PolicyWithRole result = restTemplate.exchange(
                resourceUrl,
                HttpMethod.GET,
                HTTP_ENTITY,
                PolicyWithRole.class
        ).getBody();

        return Optional.ofNullable(result);
    }

    private String getClientUuid() {
        String clientsUrl = String.format("%s/admin/realms/%s/clients", props.getServerUrl(), props.getRealm());
        ResponseEntity<List> response = restTemplate.exchange(clientsUrl, HttpMethod.GET, HTTP_ENTITY, List.class);
        List<Map<String, Object>> clients = response.getBody();

        return clients.stream()
                .filter(c -> props.getClientId().equals(c.get("clientId")))
                .map(c -> (String) c.get("id"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Client not found"));
    }

    @PostConstruct
    public void initialize() {
        HTTP_ENTITY = makeAccessHeader();
        makeClientAuthMeta();
    }

    public void refreshClientAuthMeta() {
        HTTP_ENTITY = makeAccessHeader();
        makeClientAuthMeta();
    }

    public List<Permission> getPermissions() {
        return CLIENT_AUTH_META.getPermissions();
    }

    public Optional<Resource> findResourceById(String id) {
        return CLIENT_AUTH_META.findResource(id);
    }

    public Optional<Role> findRoleById(String id) {
        return CLIENT_AUTH_META.findRole(id);
    }

}
