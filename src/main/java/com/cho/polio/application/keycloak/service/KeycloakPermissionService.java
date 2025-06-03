package com.cho.polio.application.keycloak.service;

import com.cho.polio.application.keycloak.dto.RoleRule;
import com.cho.polio.infrastructure.keycloak.client.KeycloakAdminClient;
import com.cho.polio.infrastructure.keycloak.dto.PermissionRule;
import com.cho.polio.infrastructure.keycloak.dto.PolicyWithRole;
import com.cho.polio.infrastructure.keycloak.dto._IdentityInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class KeycloakPermissionService {
    private final KeycloakAdminClient keycloakAdminClient;
    private final ResourceAssociationService resourceAssociationService;
    private final RoleAssociationService roleAssociationService;

    public List<PermissionRule> getPermissionRules() {
        return keycloakAdminClient.getPermissions()
                .stream()
                .map(PermissionRule::of)
                .map(this::buildPermissionRuleWithAssociations)
                .collect(Collectors.toList());
    }

    private PermissionRule buildPermissionRuleWithAssociations(PermissionRule permissionRule) {
        resourceAssociationService.associateResource(permissionRule);
        roleAssociationService.associateRole(permissionRule);
        return permissionRule;
    }








}
