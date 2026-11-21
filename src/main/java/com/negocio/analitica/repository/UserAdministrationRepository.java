package com.negocio.analitica.repository;

import com.negocio.analitica.model.Usuario;

/** Transactional operations used by the first-run setup and owner user management. */
public interface UserAdministrationRepository {
    boolean hasAnyUser();
    java.util.List<Usuario> findAllUsers();
    void createUserWithRole(Usuario user, String roleName);
    void setUserActive(Long userId, boolean active);
}
