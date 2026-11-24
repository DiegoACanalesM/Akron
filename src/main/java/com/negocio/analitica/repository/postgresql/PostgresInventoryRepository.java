package com.negocio.analitica.repository.postgresql;

import com.negocio.analitica.database.ConnectionProvider;
import com.negocio.analitica.model.InventoryMovement;
import com.negocio.analitica.model.InventoryMovementType;
import com.negocio.analitica.repository.PostgresInventoryOperations;
import java.sql.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** PostgreSQL stock changes and append-only movement history. */
public final class PostgresInventoryRepository implements PostgresInventoryOperations {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private final ConnectionProvider connections;

    public PostgresInventoryRepository(ConnectionProvider connections) {
        if (connections == null) throw new IllegalArgumentException("ConnectionProvider cannot be null.");
        this.connections = connections;
    }

    @Override
    public InventoryMovement registerEntry(Long productId, Long userId, int quantity, String note) {
        return changeStock(productId, userId, InventoryMovementType.ENTRY, quantity, null, note);
    }

    @Override
    public InventoryMovement registerExit(Long productId, Long userId, int quantity, String note) {
        return changeStock(productId, userId, InventoryMovementType.EXIT, quantity, null, note);
    }

    @Override
    public InventoryMovement adjustStock(Long productId, Long userId, int newStock, String note) {
        if (newStock < 0) throw new IllegalArgumentException("El stock no puede ser negativo.");
        return changeStock(productId, userId, InventoryMovementType.ADJUSTMENT, 0, newStock, note);
    }

    private InventoryMovement changeStock(Long productId, Long userId, InventoryMovementType type,
                                          int quantity, Integer targetStock, String note) {
        if (productId == null || userId == null) {
            throw new IllegalArgumentException("El producto y el usuario son obligatorios.");
        }
        if (type != InventoryMovementType.ADJUSTMENT && quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }
        try (Connection connection = connections.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureActiveOperator(connection, userId);
                int stockBefore;
                int stockAfter;
                if (type == InventoryMovementType.ENTRY || type == InventoryMovementType.EXIT) {
                    String sql = type == InventoryMovementType.ENTRY
                            ? "UPDATE products SET stock = stock + ? WHERE is_active = TRUE AND id = ? RETURNING stock"
                            : "UPDATE products SET stock = stock - ? WHERE is_active = TRUE AND id = ? AND stock >= ? RETURNING stock";
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setInt(1, quantity);
                        statement.setLong(2, productId);
                        if (type == InventoryMovementType.EXIT) statement.setInt(3, quantity);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new IllegalArgumentException(type == InventoryMovementType.EXIT
                                        ? "Producto no encontrado o stock insuficiente."
                                        : "Producto no encontrado.");
                            }
                            stockAfter = result.getInt(1);
                        }
                    }
                    stockBefore = type == InventoryMovementType.ENTRY
                            ? Math.subtractExact(stockAfter, quantity)
                            : Math.addExact(stockAfter, quantity);
                } else {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT stock FROM products WHERE id = ? AND is_active = TRUE FOR UPDATE")) {
                        statement.setLong(1, productId);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) throw new IllegalArgumentException("Producto no encontrado.");
                            stockBefore = result.getInt(1);
                        }
                    }
                    stockAfter = targetStock;
                    if (stockAfter == stockBefore) {
                        throw new IllegalArgumentException("El stock nuevo es igual al stock actual.");
                    }
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE products SET stock = ? WHERE is_active = TRUE AND id = ?")) {
                        statement.setInt(1, stockAfter);
                        statement.setLong(2, productId);
                        statement.executeUpdate();
                    }
                    quantity = Math.abs(Math.subtractExact(stockAfter, stockBefore));
                }

                InventoryMovement movement = new InventoryMovement(null, productId, userId, null,
                        type, quantity, stockBefore, stockAfter, normalizeNote(note), null);
                insertMovement(connection, movement);
                connection.commit();
                return movement;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            } finally {
                resetAutoCommit(connection);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible actualizar el inventario en PostgreSQL.", exception);
        }
    }

    /**
     * Persists a caller-provided movement only if the stored stock still equals
     * stockBefore. This prevents stale snapshots from overwriting newer stock.
     */
    @Override
    public InventoryMovement save(InventoryMovement movement) {
        validateMovement(movement);
        String update = "UPDATE products SET stock = ? WHERE is_active = TRUE AND id = ? AND stock = ? RETURNING stock";
        try (Connection connection = connections.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureActiveOperator(connection, movement.getUserId());
                try (PreparedStatement statement = connection.prepareStatement(update)) {
                    statement.setInt(1, movement.getStockAfter());
                    statement.setLong(2, movement.getProductId());
                    statement.setInt(3, movement.getStockBefore());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("El stock cambió o el producto no existe; vuelve a consultar.");
                        }
                    }
                }
                insertMovement(connection, movement);
                connection.commit();
                return movement;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            } finally {
                resetAutoCommit(connection);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible guardar el movimiento de inventario.", exception);
        }
    }

    @Override
    public List<InventoryMovement> findByProductId(Long productId) {
        if (productId == null) return List.of();
        String sql = """
                SELECT id, product_id, user_id, sale_detail_id, movement_type,
                       quantity, stock_before, stock_after, note, created_at
                FROM inventory_movements
                WHERE product_id = ?
                ORDER BY created_at DESC, id DESC
                """;
        List<InventoryMovement> movements = new ArrayList<>();
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) movements.add(map(result));
            }
            return movements;
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible consultar el historial de inventario.", exception);
        }
    }

    private static void insertMovement(Connection connection, InventoryMovement movement)
            throws SQLException {
        validateMovement(movement);
        String sql = """
                INSERT INTO inventory_movements(
                    product_id, user_id, sale_detail_id, movement_type,
                    quantity, stock_before, stock_after, note
                ) VALUES (?, ?, NULL, ?, ?, ?, ?, ?)
                RETURNING id, created_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movement.getProductId());
            statement.setLong(2, movement.getUserId());
            statement.setString(3, movement.getMovementType().name());
            statement.setInt(4, movement.getQuantity());
            statement.setInt(5, movement.getStockBefore());
            statement.setInt(6, movement.getStockAfter());
            statement.setString(7, movement.getNote());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("PostgreSQL did not return the inventory movement ID.");
                movement.setId(result.getLong("id"));
                movement.setCreatedAt(result.getTimestamp("created_at").toInstant()
                        .atZone(BUSINESS_ZONE).toLocalDateTime());
            }
        }
    }

    private static InventoryMovement map(ResultSet result) throws SQLException {
        return new InventoryMovement(result.getLong("id"), result.getLong("product_id"),
                result.getLong("user_id"), nullableLong(result, "sale_detail_id"),
                InventoryMovementType.valueOf(result.getString("movement_type")),
                result.getInt("quantity"), result.getInt("stock_before"),
                result.getInt("stock_after"), result.getString("note"),
                result.getTimestamp("created_at").toInstant().atZone(BUSINESS_ZONE).toLocalDateTime());
    }

    private static Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private static void ensureActiveOperator(Connection connection, Long userId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE id = ? AND is_active = TRUE FOR SHARE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("El usuario no está activo.");
            }
        }
    }

    private static void validateMovement(InventoryMovement movement) {
        if (movement == null || movement.getProductId() == null || movement.getUserId() == null
                || movement.getMovementType() == null) {
            throw new IllegalArgumentException("Los datos del movimiento son obligatorios.");
        }
        if (movement.getSaleDetailId() != null) {
            throw new IllegalArgumentException("Los movimientos de venta se gestionan desde el repositorio de ventas.");
        }
        if (movement.getMovementType() != InventoryMovementType.ENTRY
                && movement.getMovementType() != InventoryMovementType.EXIT
                && movement.getMovementType() != InventoryMovementType.ADJUSTMENT) {
            throw new IllegalArgumentException("Este repositorio solo acepta entradas, salidas y ajustes manuales.");
        }
        if (movement.getQuantity() <= 0 || movement.getStockBefore() < 0 || movement.getStockAfter() < 0) {
            throw new IllegalArgumentException("La cantidad y los stocks deben ser válidos.");
        }
        boolean valid = switch (movement.getMovementType()) {
            case ENTRY -> movement.getStockAfter() == movement.getStockBefore() + movement.getQuantity();
            case EXIT -> movement.getStockBefore() >= movement.getQuantity()
                    && movement.getStockAfter() == movement.getStockBefore() - movement.getQuantity();
            case ADJUSTMENT -> Math.abs((long) movement.getStockAfter() - movement.getStockBefore())
                    == movement.getQuantity();
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException("Los valores del movimiento no coinciden con su tipo.");
    }

    private static String normalizeNote(String note) {
        if (note == null || note.isBlank()) return null;
        String value = note.trim();
        if (value.length() > 255) throw new IllegalArgumentException("La nota no puede superar 255 caracteres.");
        return value;
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }

    private static void resetAutoCommit(Connection connection) {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // Closing the connection below releases it even if reset fails.
        }
    }
}
