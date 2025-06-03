package com.cho.polio.infrastructure.keycloak.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resource extends _IdentityInfo {

    private List<String> uris;
    private List<Scope> scopes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Scope {
        private String id;
        private String name;
        private String iconUri;
    }
}
