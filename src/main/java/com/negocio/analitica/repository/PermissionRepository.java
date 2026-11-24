package com.negocio.analitica.repository;

import java.util.Set;

/** Reads effective permissions after applying individual user overrides. */
public interface PermissionRepository {
    Set<String> findEffectivePermissionCodes(Long userId);
    default Set<String> findAllPermissionCodes() { return Set.of(); }
}
