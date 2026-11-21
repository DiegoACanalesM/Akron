package com.negocio.analitica.repository.postgresql;

import com.negocio.analitica.database.ConnectionProvider;
import com.negocio.analitica.model.Usuario;
import com.negocio.analitica.repository.UsuarioRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/** PostgreSQL implementation of user persistence. */
public final class PostgresUsuarioRepository implements UsuarioRepository {
    private final ConnectionProvider connections;

    public PostgresUsuarioRepository(ConnectionProvider connections) {
        if (connections == null) throw new IllegalArgumentException("ConnectionProvider is required.");
        this.connections = connections;
    }

    @Override
    public Usuario save(Usuario user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()
                || user.getPasswordHash() == null || user.getPasswordHash().isBlank()
                || user.getFullName() == null || user.getFullName().isBlank()) {
            throw new IllegalArgumentException("Los datos del usuario están incompletos.");
        }
        String sql = user.getId() == null
                ? "INSERT INTO users (username, password_hash, full_name, is_active) VALUES (?, ?, ?, ?) RETURNING id"
                : "UPDATE users SET username = ?, password_hash = ?, full_name = ?, is_active = ? WHERE id = ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = user.getId() == null
                     ? connection.prepareStatement(sql)
                     : connection.prepareStatement(sql)) {
            statement.setString(1, user.getUsername().trim());
            statement.setString(2, user.getPasswordHash());
            statement.setString(3, user.getFullName().trim());
            statement.setBoolean(4, user.isActive());
            if (user.getId() != null) statement.setLong(5, user.getId());

            if (user.getId() == null) {
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new SQLException("No se devolvió el ID del usuario creado.");
                    user.setId(result.getLong(1));
                }
            } else if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("El usuario que intentas actualizar no existe.");
            }
            return user;
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo guardar el usuario en PostgreSQL.", error);
        }
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        if (id == null) return Optional.empty();
        return findOne("SELECT id, username, password_hash, full_name, is_active FROM users WHERE id = ?", id);
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, username, password_hash, full_name, is_active FROM users WHERE username = ?")) {
            statement.setString(1, username.trim());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo consultar el usuario en PostgreSQL.", error);
        }
    }

    private Optional<Usuario> findOne(String sql, Long id) {
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo consultar el usuario en PostgreSQL.", error);
        }
    }

    private Usuario map(ResultSet result) throws SQLException {
        return new Usuario(result.getLong("id"), result.getString("username"),
                result.getString("password_hash"), result.getString("full_name"),
                result.getBoolean("is_active"));
    }
}
