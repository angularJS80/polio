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

    public List<PermissionRule> getPermissionRules() {
        return keycloakAdminClient.getPermissions()
                .stream()
                .map(PermissionRule::of)
                .map(this::buildPermissionRuleWithAssociations)
                .collect(Collectors.toList());
    }

    private PermissionRule buildPermissionRuleWithAssociations(PermissionRule permissionRule) {
        associateResource(permissionRule);
        associateRole(permissionRule);
        return permissionRule;
    }


    private PermissionRule associateResource(PermissionRule permissionRule) {
        keycloakAdminClient.retrieveResourceIdentityPermission(permissionRule.getPermissionId())
                .stream()
                .map(_IdentityInfo::get_id)
                .map(keycloakAdminClient::findResourceById)
                .flatMap(Optional::stream)
                .findFirst()
                .ifPresent(permissionRule::setResource);

        return permissionRule;
    }

    private PermissionRule associateRole(PermissionRule permissionRule) {
        keycloakAdminClient.retrievePolicyPermissionId(permissionRule.getPermissionId()).forEach(policy -> {
            permissionRule.setPolicy(policy);

            keycloakAdminClient.findPolicyWithRoleByPolicyId(policy.getId())
                    .flatMap(PolicyWithRole::findRoles)
                    .stream()
                    .flatMap(Collection::stream)
                    .map(roleConfig -> keycloakAdminClient.findRoleById(roleConfig.getId())
                            .map(role -> RoleRule.of(roleConfig, role.getName())))
                    .flatMap(Optional::stream)
                    .forEach(permissionRule::addRoleRule);
        });

        return permissionRule;
    }



}
