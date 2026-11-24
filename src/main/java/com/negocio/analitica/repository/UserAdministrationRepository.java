package com.negocio.analitica.repository;

import com.negocio.analitica.model.Usuario;
import java.util.List;

/** Transactional operations used by first-run setup and owner user management. */
public interface UserAdministrationRepository {
    boolean hasAnyUser();
    List<Usuario> findAllUsers();
    boolean isEmployee(Long userId);
    void createUserWithRole(Usuario user, String roleName);
    void setUserActive(Long userId, boolean active);
}
