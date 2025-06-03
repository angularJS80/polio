package com.cho.polio.presentation.security.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
public class PermissionRule {
    String permissionId;
    Resource resource;
    Policy policy;
    List<RoleRule> roleRules = new ArrayList<>();

    public PermissionRule(Permission permission) {
        this.permissionId = permission.getId();
    }

    public static PermissionRule of(Permission permission) {
        return new PermissionRule(permission);
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }


    public void addRoleRule(RoleRule roleRule) {
        this.roleRules.add(roleRule);
    }

    public Optional<Resource> findResource(){
        return Optional.ofNullable(this.resource);
    }
}
