package com.negocio.analitica.service;

import com.negocio.analitica.model.Usuario;
import com.negocio.analitica.repository.UserAdministrationRepository;
import com.negocio.analitica.security.AuthenticatedSession;
import com.negocio.analitica.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserManagementServiceTest {
    private static final class FakeUserRepository implements UserAdministrationRepository {
        boolean hasUsers;
        String createdRole;
        Usuario createdUser;
        Long changedUserId;
        Boolean changedActive;

        @Override public boolean hasAnyUser() { return hasUsers; }
        @Override public java.util.List<Usuario> findAllUsers() {
            return createdUser == null ? java.util.List.of() : java.util.List.of(createdUser);
        }
        @Override public void createUserWithRole(Usuario user, String roleName) {
            createdUser = user;
            createdRole = roleName;
            hasUsers = true;
        }
        @Override public void setUserActive(Long userId, boolean active) {
            changedUserId = userId;
            changedActive = active;
        }
    }

    private final PasswordHasher hasher = new PasswordHasher() {
        @Override public String hash(char[] password) { return "hashed-value"; }
        @Override public boolean verify(char[] password, String encodedHash) { return false; }
    };

    private AuthenticatedSession ownerSession() {
        return new AuthenticatedSession(1L, "owner", "Owner", Set.of("USERS_MANAGE"));
    }

    @Test
    void bootstrapCreatesOwnerOnlyForEmptyDatabaseAndClearsPassword() {
        FakeUserRepository repository = new FakeUserRepository();
        UserManagementService service = new UserManagementService(repository, hasher);
        char[] password = "long-secure-password".toCharArray();

        service.bootstrapOwner("owner", "Business Owner", password);

        assertEquals("OWNER", repository.createdRole);
        assertEquals("hashed-value", repository.createdUser.getPasswordHash());
        assertArrayEquals(new char[password.length], password);
    }

    @Test
    void bootstrapIsRejectedWhenAnyUserAlreadyExists() {
        FakeUserRepository repository = new FakeUserRepository();
        repository.hasUsers = true;
        UserManagementService service = new UserManagementService(repository, hasher);

        assertThrows(IllegalStateException.class,
                () -> service.bootstrapOwner("owner", "Business Owner",
                        "long-secure-password".toCharArray()));
        assertNull(repository.createdUser);
    }

    @Test
    void employeeCreationRequiresUsersManagePermission() {
        FakeUserRepository repository = new FakeUserRepository();
        UserManagementService service = new UserManagementService(repository, hasher);
        AuthenticatedSession employee = new AuthenticatedSession(
                2L, "employee", "Employee", Set.of("PRODUCTS_VIEW"));

        assertThrows(SecurityException.class,
                () -> service.createEmployee(employee, "newuser", "New User",
                        "long-secure-password".toCharArray()));
        assertNull(repository.createdUser);
    }

    @Test
    void ownerCanCreateEmployeeWithEmployeeRole() {
        FakeUserRepository repository = new FakeUserRepository();
        UserManagementService service = new UserManagementService(repository, hasher);

        service.createEmployee(ownerSession(), "cashier", "Cashier",
                "long-secure-password".toCharArray());

        assertEquals("EMPLOYEE", repository.createdRole);
        assertEquals("cashier", repository.createdUser.getUsername());
    }

    @Test
    void onlyAuthorizedUserCanChangeAccountStatus() {
        FakeUserRepository repository = new FakeUserRepository();
        UserManagementService service = new UserManagementService(repository, hasher);

        service.setUserActive(ownerSession(), 9L, false);

        assertEquals(9L, repository.changedUserId);
        assertEquals(Boolean.FALSE, repository.changedActive);
    }
}
