package com.negocio.analitica.service;

import com.negocio.analitica.repository.PermissionAdministrationRepository;
import com.negocio.analitica.security.AuthenticatedSession;

/** Allows an authorized owner to grant, deny, or restore employee permissions. */
public final class PermissionManagementService {
    private final PermissionAdministrationRepository permissions;

    public PermissionManagementService(PermissionAdministrationRepository permissions) {
        if (permissions == null) throw new IllegalArgumentException("Permission repository is required.");
        this.permissions = permissions;
    }

    public void grantOrDeny(AuthenticatedSession actor, Long userId,
                            String permissionCode, boolean granted) {
        requireManagementPermission(actor);
        permissions.setOverride(userId, permissionCode, granted);
    }

    public void restoreRoleDefault(AuthenticatedSession actor, Long userId,
                                   String permissionCode) {
        requireManagementPermission(actor);
        permissions.clearOverride(userId, permissionCode);
    }

    private void requireManagementPermission(AuthenticatedSession actor) {
        if (actor == null) throw new SecurityException("Debes iniciar sesión.");
        actor.requirePermission("USERS_MANAGE");
    }
}
