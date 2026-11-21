package com.negocio.analitica.repository;

/** Writes per-user permission overrides. */
public interface PermissionAdministrationRepository {
    void setOverride(Long userId, String permissionCode, boolean granted);
    void clearOverride(Long userId, String permissionCode);
}
