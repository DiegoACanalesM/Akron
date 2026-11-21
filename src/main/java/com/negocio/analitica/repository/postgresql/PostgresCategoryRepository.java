package com.negocio.analitica.repository.postgresql;

import com.negocio.analitica.database.ConnectionProvider;
import com.negocio.analitica.model.Category;
import com.negocio.analitica.repository.CategoryRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL persistence for product categories. */
public final class PostgresCategoryRepository implements CategoryRepository {
    private final ConnectionProvider connections;

    public PostgresCategoryRepository(ConnectionProvider connections) {
        if (connections == null) throw new IllegalArgumentException("ConnectionProvider cannot be null.");
        this.connections = connections;
    }

    @Override
    public Category save(Category category) {
        if (category == null) throw new IllegalArgumentException("Category cannot be null.");
        String sql = category.getId() == null
                ? "INSERT INTO categories(name, description, is_active) VALUES (?, ?, ?) RETURNING id"
                : "UPDATE categories SET name = ?, description = ?, is_active = ? WHERE id = ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            statement.setBoolean(3, category.isActive());
            if (category.getId() != null) {
                statement.setLong(4, category.getId());
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Category does not exist.");
                }
            } else {
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new SQLException("PostgreSQL did not return the category ID.");
                    category.setId(result.getLong(1));
                }
            }
            return category;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save category in PostgreSQL.", exception);
        }
    }

    @Override
    public Optional<Category> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, name, description, is_active FROM categories WHERE id = ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not find category in PostgreSQL.", exception);
        }
    }

    @Override
    public List<Category> findAll() {
        String sql = "SELECT id, name, description, is_active FROM categories ORDER BY id";
        List<Category> categories = new ArrayList<>();
        try (Connection connection = connections.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) categories.add(map(result));
            return categories;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not list categories from PostgreSQL.", exception);
        }
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) return;
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM categories WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not delete category; products may reference it.", exception);
        }
    }

    private static Category map(ResultSet result) throws SQLException {
        return new Category(result.getLong("id"), result.getString("name"),
                result.getString("description"), result.getBoolean("is_active"));
    }
}
