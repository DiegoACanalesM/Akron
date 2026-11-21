package com.negocio.analitica.service;

import com.negocio.analitica.model.Usuario;
import com.negocio.analitica.repository.PermissionRepository;
import com.negocio.analitica.repository.UsuarioRepository;
import com.negocio.analitica.security.AuthenticatedSession;
import com.negocio.analitica.security.PasswordHasher;
import java.util.Optional;

/** Authenticates active users and creates an immutable operator session. */
public final class AuthenticationService {
    private final UsuarioRepository users;
    private final PermissionRepository permissions;
    private final PasswordHasher passwordHasher;

    public AuthenticationService(UsuarioRepository users, PermissionRepository permissions,
                                 PasswordHasher passwordHasher) {
        if (users == null || permissions == null || passwordHasher == null) {
            throw new IllegalArgumentException("Authentication dependencies are required.");
        }
        this.users = users;
        this.permissions = permissions;
        this.passwordHasher = passwordHasher;
    }

    public Optional<AuthenticatedSession> login(String username, char[] password) {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            clear(password);
            return Optional.empty();
        }

        try {
            Optional<Usuario> found = users.findByUsername(username.trim());
            if (found.isEmpty()) return Optional.empty();

            Usuario user = found.get();
            if (!user.isActive() || !passwordHasher.verify(password, user.getPasswordHash())) {
                return Optional.empty();
            }

            return Optional.of(new AuthenticatedSession(
                    user.getId(), user.getUsername(), user.getFullName(),
                    permissions.findEffectivePermissionCodes(user.getId())));
        } finally {
            clear(password);
        }
    }

    private static void clear(char[] password) {
        if (password != null) java.util.Arrays.fill(password, '\0');
    }
}
