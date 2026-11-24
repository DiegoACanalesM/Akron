package com.negocio.analitica.repository.postgresql;

import com.negocio.analitica.database.ConnectionProvider;
import com.negocio.analitica.repository.PermissionRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

/** Resolves role permissions and applies user-specific grants or denials. */
public final class PostgresPermissionRepository implements PermissionRepository {
    private static final String EFFECTIVE_PERMISSIONS_SQL = """
            SELECT p.code
            FROM permissions p
            LEFT JOIN user_permissions up
                ON up.permission_id = p.id AND up.user_id = ?
            WHERE CASE
                WHEN up.user_id IS NOT NULL THEN up.granted
                ELSE EXISTS (
                    SELECT 1
                    FROM user_roles ur
                    JOIN role_permissions rp ON rp.role_id = ur.role_id
                    WHERE ur.user_id = ? AND rp.permission_id = p.id
                )
            END = TRUE
            ORDER BY p.code
            """;

    private final ConnectionProvider connections;

    public PostgresPermissionRepository(ConnectionProvider connections) {
        if (connections == null) throw new IllegalArgumentException("ConnectionProvider is required.");
        this.connections = connections;
    }

    @Override
    public Set<String> findEffectivePermissionCodes(Long userId) {
        if (userId == null) throw new IllegalArgumentException("User ID is required.");
        Set<String> permissions = new LinkedHashSet<>();
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(EFFECTIVE_PERMISSIONS_SQL)) {
            statement.setLong(1, userId);
            statement.setLong(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) permissions.add(result.getString("code"));
            }
            return Set.copyOf(permissions);
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudieron consultar los permisos del usuario.", error);
        }
    }

    @Override
    public Set<String> findAllPermissionCodes() {
        Set<String> codes = new LinkedHashSet<>();
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT code FROM permissions ORDER BY code");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) codes.add(result.getString("code"));
            return Set.copyOf(codes);
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudieron consultar los permisos disponibles.", error);
        }
    }
}
