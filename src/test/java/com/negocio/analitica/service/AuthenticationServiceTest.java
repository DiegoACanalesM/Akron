package com.negocio.analitica.service;

import com.negocio.analitica.model.Usuario;
import com.negocio.analitica.repository.PermissionRepository;
import com.negocio.analitica.repository.UsuarioRepository;
import com.negocio.analitica.security.AuthenticatedSession;
import com.negocio.analitica.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServiceTest {
    private static final String HASH = "stored-hash";

    private AuthenticationService serviceFor(Usuario user) {
        UsuarioRepository users = new UsuarioRepository() {
            @Override public Usuario save(Usuario value) { return value; }
            @Override public Optional<Usuario> findById(Long id) { return Optional.ofNullable(user); }
            @Override public Optional<Usuario> findByUsername(String username) {
                return user != null && user.getUsername().equals(username)
                        ? Optional.of(user) : Optional.empty();
            }
        };
        PermissionRepository permissions = userId -> Set.of("PRODUCTS_VIEW", "SALES_CREATE");
        PasswordHasher hasher = new PasswordHasher() {
            @Override public String hash(char[] password) { return "stored-hash"; }
            @Override public boolean verify(char[] password, String encodedHash) {
                return new String(password).equals("correct-password")
                        && HASH.equals(encodedHash);
            }
        };
        return new AuthenticationService(users, permissions, hasher);
    }

    @Test
    void activeUserCanLoginAndReceivesEffectivePermissions() {
        Usuario user = new Usuario(7L, "operator", HASH, "Store Operator", true);
        char[] password = "correct-password".toCharArray();

        Optional<AuthenticatedSession> result = serviceFor(user).login("operator", password);

        assertTrue(result.isPresent());
        assertEquals(7L, result.get().getUserId());
        assertTrue(result.get().hasPermission("SALES_CREATE"));
        assertFalse(result.get().hasPermission("USERS_MANAGE"));
        assertArrayEquals(new char[password.length], password);
    }

    @Test
    void invalidPasswordDoesNotCreateSession() {
        Usuario user = new Usuario(7L, "operator", HASH, "Store Operator", true);
        assertTrue(serviceFor(user).login("operator",
                "incorrect-password".toCharArray()).isEmpty());
    }

    @Test
    void inactiveUserCannotLoginEvenWithCorrectPassword() {
        Usuario user = new Usuario(7L, "operator", HASH, "Store Operator", false);
        assertTrue(serviceFor(user).login("operator",
                "correct-password".toCharArray()).isEmpty());
    }

    @Test
    void unknownUserCannotLogin() {
        assertTrue(serviceFor(null).login("unknown",
                "correct-password".toCharArray()).isEmpty());
    }
}
