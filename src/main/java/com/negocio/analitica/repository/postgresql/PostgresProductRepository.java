package com.negocio.analitica.repository.postgresql;

import com.negocio.analitica.database.ConnectionProvider;
import com.negocio.analitica.model.Category;
import com.negocio.analitica.model.Product;
import com.negocio.analitica.repository.ProductRepository;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PostgreSQL persistence for products and their category reference. */
public final class PostgresProductRepository implements ProductRepository {
    private final ConnectionProvider connections;

    public PostgresProductRepository(ConnectionProvider connections) {
        if (connections == null) throw new IllegalArgumentException("ConnectionProvider cannot be null.");
        this.connections = connections;
    }

    @Override
    public Product save(Product product) {
        if (product == null) throw new IllegalArgumentException("Product cannot be null.");
        if (product.getCategory() == null || product.getCategory().getId() == null) {
            throw new IllegalArgumentException("A saved product must reference a category.");
        }
        String sql = product.getId() == null
                ? """
                  INSERT INTO products(name, description, price, stock, minimum_stock, category_id, is_active)
                  VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                  """
                : """
                  UPDATE products
                  SET name = ?, description = ?, price = ?, stock = ?, minimum_stock = ?,
                      category_id = ?, is_active = ?
                  WHERE id = ?
                  """;
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, product);
            if (product.getId() != null) {
                statement.setLong(8, product.getId());
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Product does not exist.");
                }
            } else {
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new SQLException("PostgreSQL did not return the product ID.");
                    product.setId(result.getLong(1));
                }
            }
            return product;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save product in PostgreSQL.", exception);
        }
    }

    @Override
    public Optional<Product> findById(Long id) {
        if (id == null) return Optional.empty();
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql() + " WHERE p.id = ?")) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not find product in PostgreSQL.", exception);
        }
    }

    @Override
    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        try (Connection connection = connections.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(selectSql() + " ORDER BY p.id")) {
            while (result.next()) products.add(map(result));
            return products;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not list products from PostgreSQL.", exception);
        }
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) return;
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM products WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not delete product; sales or inventory movements may reference it.", exception);
        }
    }

    private static String selectSql() {
        return """
                SELECT p.id, p.name, p.description, p.price, p.stock, p.minimum_stock, p.is_active,
                       c.id AS category_id, c.name AS category_name,
                       c.description AS category_description, c.is_active AS category_is_active
                FROM products p
                JOIN categories c ON c.id = p.category_id
                """;
    }

    private static void bind(PreparedStatement statement, Product product) throws SQLException {
        statement.setString(1, product.getName());
        statement.setString(2, product.getDescription());
        statement.setBigDecimal(3, BigDecimal.valueOf(product.getPrice()));
        statement.setInt(4, product.getStock());
        statement.setInt(5, product.getMinimumStock());
        statement.setLong(6, product.getCategory().getId());
        statement.setBoolean(7, product.isActive());
    }

    private static Product map(ResultSet result) throws SQLException {
        Category category = new Category(result.getLong("category_id"),
                result.getString("category_name"), result.getString("category_description"),
                result.getBoolean("category_is_active"));
        return new Product(result.getLong("id"), result.getString("name"),
                result.getString("description"), result.getBigDecimal("price").doubleValue(),
                result.getInt("stock"), result.getInt("minimum_stock"), category,
                result.getBoolean("is_active"));
    }
}
