package com.negocio.analitica.repository.postgresql;

import com.negocio.analitica.database.ConnectionProvider;
import com.negocio.analitica.model.MetodoPago;
import com.negocio.analitica.repository.MetodoPagoRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL read access for active payment methods. */
public final class PostgresMetodoPagoRepository implements MetodoPagoRepository {
    private final ConnectionProvider connections;

    public PostgresMetodoPagoRepository(ConnectionProvider connections) {
        if (connections == null) throw new IllegalArgumentException("ConnectionProvider cannot be null.");
        this.connections = connections;
    }

    @Override
    public Optional<MetodoPago> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, name, is_active FROM payment_methods WHERE id = ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                return Optional.of(map(result));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible consultar el método de pago en PostgreSQL.", exception);
        }
    }

    @Override
    public List<MetodoPago> findAllActive() {
        String sql = "SELECT id, name, is_active FROM payment_methods WHERE is_active = TRUE ORDER BY name, id";
        List<MetodoPago> methods = new ArrayList<>();
        try (Connection connection = connections.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) methods.add(map(result));
            return methods;
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible listar métodos de pago desde PostgreSQL.", exception);
        }
    }

    private static MetodoPago map(ResultSet result) throws SQLException {
        return new MetodoPago(result.getLong("id"), result.getString("name"),
                result.getBoolean("is_active"));
    }
}
