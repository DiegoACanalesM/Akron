package com.negocio.analitica.service;

import com.negocio.analitica.model.Usuario;
import com.negocio.analitica.repository.UserAdministrationRepository;
import com.negocio.analitica.security.AuthenticatedSession;
import com.negocio.analitica.security.PasswordHasher;
import java.util.List;

/** First-owner setup and owner-authorized employee administration. */
public final class UserManagementService {
    private final UserAdministrationRepository users;
    private final PasswordHasher passwordHasher;

    public UserManagementService(UserAdministrationRepository users, PasswordHasher passwordHasher) {
        if (users == null || passwordHasher == null) {
            throw new IllegalArgumentException("User-management dependencies are required.");
        }
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    /** Creates the initial OWNER only while the database has no users. */
    public void bootstrapOwner(String username, String fullName, char[] password) {
        try {
            validateUserData(username, fullName, password);
            if (users.hasAnyUser()) {
                throw new IllegalStateException(
                        "La configuración inicial ya se realizó. Solo un propietario puede crear usuarios.");
            }
            String passwordHash = passwordHasher.hash(password);
            users.createUserWithRole(
                    new Usuario(null, username.trim(), passwordHash, fullName.trim(), true), "OWNER");
        } finally {
            clear(password);
        }
    }

    public List<Usuario> listUsers(AuthenticatedSession actor) {
        requireUserManagement(actor);
        return users.findAllUsers();
    }

    /** Creates an employee account. Only a session with USERS_MANAGE may call this operation. */
    public void createEmployee(AuthenticatedSession actor, String username,
                               String fullName, char[] password) {
        try {
            requireUserManagement(actor);
            validateUserData(username, fullName, password);
            String passwordHash = passwordHasher.hash(password);
            users.createUserWithRole(
                    new Usuario(null, username.trim(), passwordHash, fullName.trim(), true), "EMPLOYEE");
        } finally {
            clear(password);
        }
    }

    public void setUserActive(AuthenticatedSession actor, Long userId, boolean active) {
        requireUserManagement(actor);
        users.setUserActive(userId, active);
    }

    private void requireUserManagement(AuthenticatedSession actor) {
        if (actor == null) throw new SecurityException("Debes iniciar sesión.");
        actor.requirePermission("USERS_MANAGE");
    }

    private void validateUserData(String username, String fullName, char[] password) {
        if (username == null || username.isBlank() || username.trim().length() > 60) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio y admite máximo 60 caracteres.");
        }
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 150) {
            throw new IllegalArgumentException("El nombre completo es obligatorio y admite máximo 150 caracteres.");
        }
        if (password == null || password.length < 10) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 10 caracteres.");
        }
    }

    private static void clear(char[] password) {
        if (password != null) java.util.Arrays.fill(password, '\0');
    }
}
