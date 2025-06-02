package com.cho.polio.presentation.security.dto;

import java.util.List;
import java.util.Map;

public class ResourceMetadata {
    private final Map<String, String> resourceIdToUri;
    private final Map<String, List<String>> resourceIdToScopes;

    public ResourceMetadata(Map<String, String> resourceIdToUri, Map<String, List<String>> resourceIdToScopes) {
        this.resourceIdToUri = resourceIdToUri;
        this.resourceIdToScopes = resourceIdToScopes;
    }

    public Map<String, String> getResourceIdToUri() {
        return resourceIdToUri;
    }

    public Map<String, List<String>> getResourceIdToScopes() {
        return resourceIdToScopes;
    }
}
