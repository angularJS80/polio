package com.cho.polio.application.keycloak.service;

import com.cho.polio.application.keycloak.dto.RoleRule;
import com.cho.polio.infrastructure.keycloak.client.KeycloakAdminClient;
import com.cho.polio.infrastructure.keycloak.dto.PermissionRule;
import com.cho.polio.infrastructure.keycloak.dto.PolicyWithRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleAssociationService {
    private final KeycloakAdminClient keycloakAdminClient;
    public PermissionRule associateRole(PermissionRule permissionRule) {
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
