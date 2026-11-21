package com.negocio.analitica.security;

import java.util.Set;

/** Immutable identity and effective permissions for the currently logged-in operator. */
public final class AuthenticatedSession {
    private final Long userId;
    private final String username;
    private final String fullName;
    private final Set<String> permissions;

    public AuthenticatedSession(Long userId, String username, String fullName,
                                Set<String> permissions) {
        if (userId == null || username == null || fullName == null || permissions == null) {
            throw new IllegalArgumentException("Session data cannot be null.");
        }
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.permissions = Set.copyOf(permissions);
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public Set<String> getPermissions() { return permissions; }

    public boolean hasPermission(String permissionCode) {
        return permissionCode != null && permissions.contains(permissionCode);
    }

    public void requirePermission(String permissionCode) {
        if (!hasPermission(permissionCode)) {
            throw new SecurityException("No tienes permiso para realizar esta operación.");
        }
    }
}
