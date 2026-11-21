package com.negocio.analitica.repository.postgresql;

import com.negocio.analitica.database.ConnectionProvider;
import com.negocio.analitica.model.Cliente;
import com.negocio.analitica.repository.ClienteRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL persistence for customers. */
public final class PostgresClienteRepository implements ClienteRepository {
    private final ConnectionProvider connections;

    public PostgresClienteRepository(ConnectionProvider connections) {
        if (connections == null) throw new IllegalArgumentException("ConnectionProvider cannot be null.");
        this.connections = connections;
    }

    @Override
    public Cliente save(Cliente cliente) {
        if (cliente == null) throw new IllegalArgumentException("Cliente cannot be null.");
        String sql = cliente.getId() == null
                ? "INSERT INTO customers(name, document, phone, address) VALUES (?, ?, ?, ?) RETURNING id"
                : "UPDATE customers SET name = ?, document = ?, phone = ?, address = ? WHERE id = ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, cliente);
            if (cliente.getId() != null) {
                statement.setLong(5, cliente.getId());
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Cliente no existe.");
                }
            } else {
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new SQLException("PostgreSQL did not return the customer ID.");
                    cliente.setId(result.getLong(1));
                }
            }
            return cliente;
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible guardar el cliente en PostgreSQL.", exception);
        }
    }

    @Override
    public Optional<Cliente> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, name, document, phone, address FROM customers WHERE id = ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible consultar el cliente en PostgreSQL.", exception);
        }
    }

    @Override
    public Optional<Cliente> findByDocumento(String documento) {
        if (documento == null || documento.isBlank()) return Optional.empty();
        String sql = "SELECT id, name, document, phone, address FROM customers WHERE document = ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, documento.trim());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible buscar el documento en PostgreSQL.", exception);
        }
    }

    @Override
    public List<Cliente> findAll() {
        String sql = "SELECT id, name, document, phone, address FROM customers ORDER BY name, id";
        List<Cliente> customers = new ArrayList<>();
        try (Connection connection = connections.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) customers.add(map(result));
            return customers;
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible listar clientes desde PostgreSQL.", exception);
        }
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) return;
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM customers WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "No se puede eliminar el cliente: puede tener ventas asociadas.", exception);
        }
    }

    private static void bind(PreparedStatement statement, Cliente cliente) throws SQLException {
        statement.setString(1, cliente.getNombre());
        String document = cliente.getDocumento();
        if (document == null || document.isBlank()) statement.setNull(2, Types.VARCHAR);
        else statement.setString(2, document.trim());
        statement.setString(3, cliente.getTelefono());
        statement.setString(4, cliente.getDireccion());
    }

    private static Cliente map(ResultSet result) throws SQLException {
        return new Cliente(result.getLong("id"), result.getString("name"),
                result.getString("document"), result.getString("phone"), result.getString("address"));
    }
}
