package com.negocio.analitica.service;

import com.negocio.analitica.repository.PermissionAdministrationRepository;
import com.negocio.analitica.security.AuthenticatedSession;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionManagementServiceTest {
    private static final class FakePermissionRepository implements PermissionAdministrationRepository {
        Long userId;
        String code;
        Boolean granted;
        boolean cleared;

        @Override public void setOverride(Long userId, String permissionCode, boolean granted) {
            this.userId = userId;
            this.code = permissionCode;
            this.granted = granted;
        }

        @Override public void clearOverride(Long userId, String permissionCode) {
            this.userId = userId;
            this.code = permissionCode;
            this.cleared = true;
        }
    }

    @Test
    void ownerCanGrantOrDenyIndividualPermission() {
        FakePermissionRepository repository = new FakePermissionRepository();
        PermissionManagementService service = new PermissionManagementService(repository);
        AuthenticatedSession owner = new AuthenticatedSession(
                1L, "owner", "Owner", Set.of("USERS_MANAGE"));

        service.grantOrDeny(owner, 8L, "SALES_CREATE", false);

        assertEquals(8L, repository.userId);
        assertEquals("SALES_CREATE", repository.code);
        assertEquals(Boolean.FALSE, repository.granted);
    }

    @Test
    void employeeCannotChangePermissions() {
        FakePermissionRepository repository = new FakePermissionRepository();
        PermissionManagementService service = new PermissionManagementService(repository);
        AuthenticatedSession employee = new AuthenticatedSession(
                2L, "employee", "Employee", Set.of("PRODUCTS_VIEW"));

        assertThrows(SecurityException.class,
                () -> service.grantOrDeny(employee, 8L, "SALES_CREATE", true));
        assertNull(repository.userId);
    }

    @Test
    void ownerCanRestoreRoleDefault() {
        FakePermissionRepository repository = new FakePermissionRepository();
        PermissionManagementService service = new PermissionManagementService(repository);
        AuthenticatedSession owner = new AuthenticatedSession(
                1L, "owner", "Owner", Set.of("USERS_MANAGE"));

        service.restoreRoleDefault(owner, 8L, "SALES_CREATE");

        assertTrue(repository.cleared);
        assertEquals(8L, repository.userId);
    }
}
