package com.cho.polio.presentation.security.util;

import java.util.Set;

public class HttpMethodScopeMapper {

    public static String mapHttpMethodToScope(String scope) {
        if (scope == null) return null;
        String lower = scope.toLowerCase();

        if (containsAny(lower, "read", "view", "list", "get", "fetch", "query", "search")) return "read";
        if (containsAny(lower, "write", "create", "add", "post", "register", "insert")) return "write";
        if (containsAny(lower, "edit", "modify", "update", "change", "patch")) return "edit";
        if (containsAny(lower, "remove", "delete", "destroy", "erase", "drop")) return "remove";

        return null;
    }

    private static boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) return true;
        }
        return false;
    }

    public static Set<String> allowedMethodsForScope(String mappedScope) {
        return switch (mappedScope) {
            case "read" -> Set.of("GET");
            case "write" -> Set.of("POST", "PUT", "PATCH");
            case "edit" -> Set.of("PATCH");
            case "remove" -> Set.of("DELETE");
            default -> Set.of();
        };
    }
}
