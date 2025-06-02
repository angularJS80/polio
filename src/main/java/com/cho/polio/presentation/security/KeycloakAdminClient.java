package com.cho.polio.presentation.security;

import com.cho.polio.presentation.security.dto.ResourceMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(KeycloakSecurityProperties.class)
public class KeycloakAdminClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final KeycloakSecurityProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Map<String, Object>> CACHED_POLICY_MAP;
    private String CLIENT_UUID ;
    private HttpEntity<?> HTTP_ENTITY ;
    // 클라이언트 역할 ID → 역할명 캐싱
    private Map<String, String> cachedRoleIdNameMap;

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

    public Map<String, List<String>> buildUriRoleMap(String token) {
        HTTP_ENTITY = makeEntity(token);
        CLIENT_UUID = getClientUuid(); // 먼저 호출되어야 함
        CACHED_POLICY_MAP = new HashMap<>();

        getPolicyResp()
                .forEach(policy -> CACHED_POLICY_MAP.put((String) policy.get("id"), policy));

        // 0. 역할 맵 캐싱 (roleId → roleName)
        cachedRoleIdNameMap = fetchClientRoleIdNameMap();

        // 1. Get all resources
        ResourceMetadata resourceMetadata = getResourceMetadata();

        // 2. Get all permissions
        return getAllPermissions().stream()
                .filter(perm -> perm.get("type") != null)
                .map(perm -> processByType(perm,  resourceMetadata))
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new ArrayList<>(e.getValue()),
                        (existing, incoming) -> {
                            existing.addAll(incoming);
                            return existing;
                        }
                ));

    }

    private HttpEntity<?> makeEntity(String token) {


        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private Map<String, Set<String>> processByType(Map<String, Object> perm,
                                                   ResourceMetadata resourceMetadata) {

        String type = (String) perm.get("type");
        switch (type) {
            case "resource":
                return processResourcePermission(perm,    resourceMetadata);

            case "scope":
                return processScopePermission(perm,  resourceMetadata);

            case "resource-scope":
            case "resource-scope-based":
                return processResourceScopePermission(perm,    resourceMetadata);

            default:
                System.out.println("Unknown permission type: " + type);
        }
        return null;
    }


    public ResourceMetadata getResourceMetadata() {
        Map<String, String> resourceIdToUri = new HashMap<>();
        Map<String, List<String>> resourceIdToScopes = new HashMap<>();

        getAllResources().forEach(res -> {
            String resourceId = (String) res.get("_id");
            List<String> uris = (List<String>) res.get("uris");
            List<Map<String, Object>> scopes = (List<Map<String, Object>>) res.get("scopes");

            List<String> scopeNames = scopes != null
                    ? scopes.stream().map(s -> (String) s.get("name")).collect(Collectors.toList())
                    : List.of();

            if (uris != null && !uris.isEmpty()) {
                resourceIdToUri.put(resourceId, uris.get(0));
                resourceIdToScopes.put(resourceId, scopeNames);
            }
        });

        return new ResourceMetadata(resourceIdToUri, resourceIdToScopes);
    }


    private  List<Map<String, Object>> getAllPermissions( ) {
        String permUrl = String.format("%s/admin/realms/%s/clients/%s/authz/resource-server/permission",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID);

        ResponseEntity<List> permResp = restTemplate.exchange(permUrl, HttpMethod.GET, HTTP_ENTITY, List.class);
       return permResp.getBody();

    }

    private List<Map<String, Object>> getPolicyResp() {
        String policyListUrl = String.format(
                "%s/admin/realms/%s/clients/%s/authz/resource-server/policy",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID
        );
        return restTemplate.exchange(policyListUrl, HttpMethod.GET, HTTP_ENTITY, List.class).getBody();
    }

    private List<Map<String, Object>> getAllResources( ) {

        String resourceUrl = String.format("%s/admin/realms/%s/clients/%s/authz/resource-server/resource",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID);
        return restTemplate.exchange(resourceUrl, HttpMethod.GET, HTTP_ENTITY, List.class).getBody();
    }

    private Map<String, String> fetchClientRoleIdNameMap() {
        String rolesUrl = String.format("%s/admin/realms/%s/clients/%s/roles",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID);

        ResponseEntity<List> rolesResp = restTemplate.exchange(rolesUrl, HttpMethod.GET, HTTP_ENTITY, List.class);
        List<Map<String, Object>> roles = rolesResp.getBody();

        Map<String, String> map = new HashMap<>();
        for (Map<String, Object> role : roles) {
            String id = (String) role.get("id");
            String name = (String) role.get("name");
            map.put(id, name);
        }
        return map;
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

    private Map<String, Set<String>> processResourcePermission(
            Map<String, Object> perm,
            ResourceMetadata resourceMetadata
    ) {

        Map<String, Set<String>> uriToRoles = new HashMap<>();
        List<Map<String, Object>> resList = (List<Map<String, Object>>) perm.get("resources");
        Set<String> roles = extractRolesFromPermission(perm);

        if (resList == null || roles.isEmpty()) return uriToRoles;

        for (Map<String, Object> r : resList) {
            String resourceId = (String) r.get("id");
            String uri = resourceMetadata.getResourceIdToUri().get(resourceId);
            if (uri != null) {
                uriToRoles.computeIfAbsent(uri, k -> new HashSet<>()).addAll(roles);
            }
        }
        return uriToRoles;
    }

    private Map<String, Set<String>> processScopePermission(
            Map<String, Object> perm,
            ResourceMetadata resourceMetadata
    ) {
        Map<String, Set<String>> uriToRoles = new HashMap<>();
        String permissionId = (String) perm.get("id");
        if (permissionId == null) return uriToRoles;

        // 1. 권한과 연관된 정책 목록을 별도 API 호출로 조회
        String assocPolicyUrl = String.format("%s/admin/realms/%s/clients/%s/authz/resource-server/policy/%s/associatedPolicies",
                props.getServerUrl(), props.getRealm(), CLIENT_UUID, permissionId);

        ResponseEntity<List> assocPoliciesResp = restTemplate.exchange(assocPolicyUrl, HttpMethod.GET, HTTP_ENTITY, List.class);
        List<Map<String, Object>> assocPolicies = assocPoliciesResp.getBody();
        if (assocPolicies == null || assocPolicies.isEmpty()) return uriToRoles;

        Set<String> roles = new HashSet<>();
        Set<String> permScopes = new HashSet<>();

        // 2. 연관 정책들을 돌면서 scopes, roles 수집
        for (Map<String, Object> policy : assocPolicies) {
            String policyId = (String) policy.get("id");
            Map<String, Object> cachedPolicy = CACHED_POLICY_MAP.get(policyId);
            if (cachedPolicy == null) continue;

            String policyType = (String) cachedPolicy.get("type");
            Map<String, String> config = (Map<String, String>) cachedPolicy.get("config");

            if ("scope".equals(policyType)) {
                if (config != null) {
                    String scopesStr = config.get("scopes");
                    if (scopesStr != null && !scopesStr.isBlank()) {
                        String[] scopesArr = scopesStr.split(",");
                        for (String s : scopesArr) permScopes.add(s.trim());
                    }
                }
            } else if ("role".equals(policyType)) {
                if (config != null) {
                    String rolesJson = config.get("roles");
                    if (rolesJson != null && !rolesJson.isBlank()) {
                        try {
                            List<Map<String, Object>> roleObjs = objectMapper.readValue(rolesJson, new TypeReference<List<Map<String, Object>>>() {
                            });
                            for (Map<String, Object> roleObj : roleObjs) {
                                String roleId = (String) roleObj.get("id");
                                if (roleId != null) {
                                    String roleName = cachedRoleIdNameMap.get(roleId);
                                    roles.add(roleName != null ? roleName : roleId);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to parse roles JSON for policy " + policyId + ": " + e.getMessage());
                        }
                    }
                }
            }
        }

        if (permScopes.isEmpty() && roles.isEmpty()) return uriToRoles;

        for (Map.Entry<String, List<String>> entry : resourceMetadata.getResourceIdToScopes().entrySet()) {
            boolean scopeMatches = !Collections.disjoint(entry.getValue(), permScopes);
            boolean roleExists = !roles.isEmpty();

            if (scopeMatches || roleExists) {
                String uri = resourceMetadata.getResourceIdToUri().get(entry.getKey());
                if (uri != null) {
                    uriToRoles.computeIfAbsent(uri, k -> new HashSet<>()).addAll(roles);
                }
            }
        }

        return uriToRoles;

    }


    private Set<String> extractRolesFromPermission(
            Map<String, Object> perm
    ) {
        Set<String> roles = new HashSet<>();

        // 권한의 연결된 정책 ID 가져오기
        Map<String, Object> config = (Map<String, Object>) perm.get("config");
        if (config == null || !config.containsKey("applyPolicies")) return roles;

        List<String> policyNames = (List<String>) config.get("applyPolicies");

        for (String name : policyNames) {
            Optional<Map<String, Object>> optPolicy = CACHED_POLICY_MAP.values().stream()
                    .filter(p -> name.equals(p.get("name")))
                    .findFirst();

            if (optPolicy.isEmpty()) continue;

            Map<String, Object> policy = optPolicy.get();
            String rolesJson = (String) policy.get("roles");

            if (rolesJson != null && !rolesJson.isEmpty()) {
                try {
                    List<Map<String, Object>> roleDefs = objectMapper.readValue(
                            rolesJson, new TypeReference<List<Map<String, Object>>>() {
                            });
                    for (Map<String, Object> roleDef : roleDefs) {
                        String roleId = (String) roleDef.get("id");
                        if (cachedRoleIdNameMap.containsKey(roleId)) {
                            roles.add(cachedRoleIdNameMap.get(roleId));
                        }
                    }
                } catch (Exception e) {
                    System.out.println(">> Error parsing role json: " + e.getMessage());
                }
            }
        }

        return roles;
    }


    private Map<String, Set<String>> processResourceScopePermission(
            Map<String, Object> perm,ResourceMetadata resourceMetadata
    ) {
        Map<String, Set<String>>  uriToRoles = new HashMap<>();
        List<Map<String, Object>> resList = (List<Map<String, Object>>) perm.get("resources");
        List<String> permScopes = (List<String>) perm.get("scopes");
        Set<String> roles = extractRolesFromPermission(perm);

        if (resList == null || permScopes == null || roles.isEmpty()) return uriToRoles;

        for (Map<String, Object> resource : resList) {
            String resourceId = (String) resource.get("id");
            String uri = resourceMetadata.getResourceIdToUri().get(resourceId);
            List<String> resourceScopes = resourceMetadata.getResourceIdToScopes().getOrDefault(resourceId, List.of());

            if (uri != null && !Collections.disjoint(resourceScopes, permScopes)) {
                uriToRoles.computeIfAbsent(uri, k -> new HashSet<>()).addAll(roles);
            }
        }
        return uriToRoles;
    }
}
