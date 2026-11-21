package com.negocio.analitica.repository.postgresql;

import com.negocio.analitica.database.ConnectionProvider;
import com.negocio.analitica.repository.PermissionAdministrationRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Persists explicit permission grants and denials for individual employees. */
public final class PostgresPermissionAdministrationRepository
        implements PermissionAdministrationRepository {
    private final ConnectionProvider connections;

    public PostgresPermissionAdministrationRepository(ConnectionProvider connections) {
        if (connections == null) throw new IllegalArgumentException("ConnectionProvider is required.");
        this.connections = connections;
    }

    @Override
    public void setOverride(Long userId, String permissionCode, boolean granted) {
        validate(userId, permissionCode);
        String sql = """
                INSERT INTO user_permissions (user_id, permission_id, granted)
                SELECT ?, p.id, ?
                FROM permissions p
                WHERE p.code = ?
                  AND EXISTS (
                      SELECT 1 FROM user_roles ur
                      JOIN roles r ON r.id = ur.role_id
                      WHERE ur.user_id = ? AND r.name = 'EMPLOYEE'
                  )
                ON CONFLICT (user_id, permission_id)
                DO UPDATE SET granted = EXCLUDED.granted
                """;
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setBoolean(2, granted);
            statement.setString(3, permissionCode.trim());
            statement.setLong(4, userId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("El usuario o el permiso indicado no existe.");
            }
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo actualizar el permiso del usuario.", error);
        }
    }

    @Override
    public void clearOverride(Long userId, String permissionCode) {
        validate(userId, permissionCode);
        String sql = """
                DELETE FROM user_permissions
                WHERE user_id = ?
                  AND permission_id = (SELECT id FROM permissions WHERE code = ?)
                  AND EXISTS (
                      SELECT 1 FROM user_roles ur
                      JOIN roles r ON r.id = ur.role_id
                      WHERE ur.user_id = user_permissions.user_id AND r.name = 'EMPLOYEE'
                  )
                """;
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, permissionCode.trim());
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo restablecer el permiso del usuario.", error);
        }
    }

    private void validate(Long userId, String permissionCode) {
        if (userId == null || permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException("El usuario y el código del permiso son obligatorios.");
        }
    }
}
