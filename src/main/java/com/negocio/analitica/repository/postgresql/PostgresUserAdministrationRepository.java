package com.negocio.analitica.repository.postgresql;

import com.negocio.analitica.database.ConnectionProvider;
import com.negocio.analitica.model.Usuario;
import com.negocio.analitica.repository.UserAdministrationRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** PostgreSQL user-administration operations with transactional role assignment. */
public final class PostgresUserAdministrationRepository implements UserAdministrationRepository {
    private final ConnectionProvider connections;

    public PostgresUserAdministrationRepository(ConnectionProvider connections) {
        if (connections == null) throw new IllegalArgumentException("ConnectionProvider is required.");
        this.connections = connections;
    }

    @Override
    public boolean hasAnyUser() {
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT EXISTS (SELECT 1 FROM users)");
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getBoolean(1);
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo verificar la configuración inicial de usuarios.", error);
        }
    }

    @Override
    public void createUserWithRole(Usuario user, String roleName) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()
                || user.getPasswordHash() == null || user.getPasswordHash().isBlank()
                || user.getFullName() == null || user.getFullName().isBlank()
                || roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Los datos del usuario están incompletos.");
        }

        try (Connection connection = connections.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if ("OWNER".equals(roleName)) {
                    // Serialize first-owner creation and guarantee that only one bootstrap succeeds.
                    try (PreparedStatement lock = connection.prepareStatement(
                            "SELECT pg_advisory_xact_lock(73420119)")) {
                        lock.execute();
                    }
                    try (PreparedStatement check = connection.prepareStatement(
                            "SELECT EXISTS (SELECT 1 FROM users)");
                         ResultSet result = check.executeQuery()) {
                        result.next();
                        if (result.getBoolean(1)) {
                            throw new IllegalStateException("La cuenta inicial OWNER ya fue creada.");
                        }
                    }
                }

                long userId;
                String insertUser = """
                        INSERT INTO users (username, password_hash, full_name, is_active)
                        VALUES (?, ?, ?, TRUE)
                        RETURNING id
                        """;
                try (PreparedStatement statement = connection.prepareStatement(insertUser)) {
                    statement.setString(1, user.getUsername().trim());
                    statement.setString(2, user.getPasswordHash());
                    statement.setString(3, user.getFullName().trim());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("No se devolvió el ID del usuario.");
                        userId = result.getLong(1);
                    }
                }

                String assignRole = """
                        INSERT INTO user_roles (user_id, role_id)
                        SELECT ?, id FROM roles WHERE name = ?
                        """;
                try (PreparedStatement statement = connection.prepareStatement(assignRole)) {
                    statement.setLong(1, userId);
                    statement.setString(2, roleName);
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException("El rol solicitado no existe.");
                    }
                }

                connection.commit();
                user.setId(userId);
                user.setActive(true);
            } catch (Exception error) {
                rollback(connection, error);
                if (error instanceof RuntimeException runtime) throw runtime;
                throw new IllegalStateException("No se pudo crear el usuario y asignar su rol.", error);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo crear el usuario en PostgreSQL.", error);
        }
    }

    @Override
    public void setUserActive(Long userId, boolean active) {
        if (userId == null) throw new IllegalArgumentException("User ID is required.");
        try (Connection connection = connections.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Serialize owner deactivations so two concurrent requests cannot disable all owners.
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT pg_advisory_xact_lock(73420120)")) {
                    lock.execute();
                }
                boolean exists;
                String lockUser = "SELECT id FROM users WHERE id = ? FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(lockUser)) {
                    statement.setLong(1, userId);
                    try (ResultSet result = statement.executeQuery()) {
                        exists = result.next();
                    }
                }
                if (!exists) throw new IllegalArgumentException("El usuario no existe.");

                if (!active && isActiveOwner(connection, userId)) {
                    long activeOwners = countActiveOwners(connection);
                    if (activeOwners <= 1) {
                        throw new IllegalStateException(
                                "No se puede desactivar al último propietario activo del negocio.");
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE users SET is_active = ? WHERE id = ?")) {
                    statement.setBoolean(1, active);
                    statement.setLong(2, userId);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (Exception error) {
                rollback(connection, error);
                if (error instanceof RuntimeException runtime) throw runtime;
                throw new IllegalStateException("No se pudo actualizar el estado del usuario.", error);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo actualizar el usuario en PostgreSQL.", error);
        }
    }

    private boolean isActiveOwner(Connection connection, Long userId) throws SQLException {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM user_roles ur
                    JOIN roles r ON r.id = ur.role_id
                    WHERE ur.user_id = ? AND r.name = 'OWNER'
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private long countActiveOwners(Connection connection) throws SQLException {
        String sql = """
                SELECT COUNT(DISTINCT u.id)
                FROM users u
                JOIN user_roles ur ON ur.user_id = u.id
                JOIN roles r ON r.id = ur.role_id
                WHERE r.name = 'OWNER' AND u.is_active = TRUE
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }
}
