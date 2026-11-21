package com.negocio.analitica.repository.postgresql;

import com.negocio.analitica.database.ConnectionProvider;
import com.negocio.analitica.model.DetalleVenta;
import com.negocio.analitica.model.Venta;
import com.negocio.analitica.repository.PostgresSaleOperations;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL sale persistence. A completed sale, its details, stock deductions
 * and SALE inventory movements are committed as one database transaction.
 */
public final class PostgresVentaRepository implements PostgresSaleOperations {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private final ConnectionProvider connections;

    public PostgresVentaRepository(ConnectionProvider connections) {
        if (connections == null) throw new IllegalArgumentException("ConnectionProvider cannot be null.");
        this.connections = connections;
    }

    @Override
    public Venta save(Venta sale) {
        validate(sale);
        String insertSale = """
                INSERT INTO sales(customer_id, user_id, payment_method_id, sale_date, status)
                VALUES (?, ?, ?, COALESCE(?::timestamptz, CURRENT_TIMESTAMP), 'COMPLETED')
                RETURNING id, sale_date
                """;
        String updateStock = """
                UPDATE products p
                SET stock = p.stock - ?
                FROM categories c
                WHERE p.category_id = c.id AND c.is_active = TRUE
                  AND p.id = ? AND p.is_active = TRUE AND p.stock >= ?
                RETURNING p.stock
                """;
        String insertDetail = """
                INSERT INTO sale_details(sale_id, product_id, product_name, category_name, quantity, unit_price)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        String insertMovement = """
                INSERT INTO inventory_movements(
                    product_id, user_id, sale_detail_id, movement_type,
                    quantity, stock_before, stock_after, note
                ) VALUES (?, ?, ?, 'SALE', ?, ?, ?, ?)
                """;

        try (Connection connection = connections.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureActiveOperator(connection, sale.getUserId());
                ensureActivePaymentMethod(connection, sale.getPaymentMethodId());
                if (sale.getClienteId() != null) ensureActiveCustomer(connection, sale.getClienteId());

                try (PreparedStatement statement = connection.prepareStatement(insertSale)) {
                    if (sale.getClienteId() == null) statement.setNull(1, Types.BIGINT);
                    else statement.setLong(1, sale.getClienteId());
                    statement.setLong(2, sale.getUserId());
                    statement.setLong(3, sale.getPaymentMethodId());
                    if (sale.getFecha() == null) statement.setNull(4, Types.TIMESTAMP_WITH_TIMEZONE);
                    else statement.setObject(4, sale.getFecha().atZone(BUSINESS_ZONE).toOffsetDateTime());
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("PostgreSQL did not return the sale ID.");
                        sale.setId(result.getLong("id"));
                        sale.setFecha(result.getTimestamp("sale_date").toInstant()
                                .atZone(BUSINESS_ZONE).toLocalDateTime());
                    }
                }

                for (DetalleVenta detail : sale.getDetalles()) {
                    int stockAfter;
                    try (PreparedStatement statement = connection.prepareStatement(updateStock)) {
                        statement.setInt(1, detail.getCantidad());
                        statement.setLong(2, detail.getProductoId());
                        statement.setInt(3, detail.getCantidad());
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new IllegalArgumentException(
                                        "Stock insuficiente o producto inactivo: " + detail.getProductoNombre());
                            }
                            stockAfter = result.getInt(1);
                        }
                    }
                    int stockBefore = Math.addExact(stockAfter, detail.getCantidad());

                    try (PreparedStatement statement = connection.prepareStatement(insertDetail)) {
                        statement.setLong(1, sale.getId());
                        statement.setLong(2, detail.getProductoId());
                        statement.setString(3, detail.getProductoNombre());
                        statement.setString(4, detail.getCategoriaNombre());
                        statement.setInt(5, detail.getCantidad());
                        statement.setBigDecimal(6, BigDecimal.valueOf(detail.getPrecioUnitario()));
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) throw new SQLException("PostgreSQL did not return the sale detail ID.");
                            detail.setId(result.getLong(1));
                        }
                    }

                    try (PreparedStatement statement = connection.prepareStatement(insertMovement)) {
                        statement.setLong(1, detail.getProductoId());
                        statement.setLong(2, sale.getUserId());
                        statement.setLong(3, detail.getId());
                        statement.setInt(4, detail.getCantidad());
                        statement.setInt(5, stockBefore);
                        statement.setInt(6, stockAfter);
                        statement.setString(7, "Sale #" + sale.getId());
                        statement.executeUpdate();
                    }
                }

                sale.setStatus("COMPLETED");
                connection.commit();
                return sale;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // Closing the connection below releases it even if reset fails.
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible registrar la venta en PostgreSQL.", exception);
        }
    }


    /**
     * Cancels a completed sale and restores every affected product's stock.
     * The sale status, stock restorations and reversal movements are atomic.
     */
    @Override
    public Venta cancelCompletedSale(Long saleId, Long operatorId, String note) {
        if (saleId == null || operatorId == null) {
            throw new IllegalArgumentException("La venta y el usuario que cancela son obligatorios.");
        }
        String lockSale = "SELECT status FROM sales WHERE id = ? FOR UPDATE";
        String lockDetails = """
                SELECT id, product_id, quantity
                FROM sale_details WHERE sale_id = ? ORDER BY id
                FOR UPDATE
                """;
        String restoreStock = "UPDATE products SET stock = stock + ? WHERE id = ? RETURNING stock";
        String insertReversal = """
                INSERT INTO inventory_movements(
                    product_id, user_id, sale_detail_id, movement_type,
                    quantity, stock_before, stock_after, note
                ) VALUES (?, ?, ?, 'SALE_REVERSAL', ?, ?, ?, ?)
                """;
        String updateStatus = "UPDATE sales SET status = 'CANCELLED' WHERE id = ?";
        try (Connection connection = connections.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureActiveOperator(connection, operatorId);
                try (PreparedStatement statement = connection.prepareStatement(lockSale)) {
                    statement.setLong(1, saleId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Venta no encontrada.");
                        if (!"COMPLETED".equals(result.getString("status"))) {
                            throw new IllegalArgumentException("Solo se pueden cancelar ventas completadas.");
                        }
                    }
                }

                try (PreparedStatement details = connection.prepareStatement(lockDetails);
                     PreparedStatement stock = connection.prepareStatement(restoreStock);
                     PreparedStatement movement = connection.prepareStatement(insertReversal)) {
                    details.setLong(1, saleId);
                    try (ResultSet result = details.executeQuery()) {
                        boolean foundDetail = false;
                        while (result.next()) {
                            foundDetail = true;
                            long detailId = result.getLong("id");
                            long productId = result.getLong("product_id");
                            int quantity = result.getInt("quantity");
                            int stockAfter;
                            stock.setInt(1, quantity);
                            stock.setLong(2, productId);
                            try (ResultSet updated = stock.executeQuery()) {
                                if (!updated.next()) throw new SQLException("Product referenced by sale detail was not found.");
                                stockAfter = updated.getInt(1);
                            }
                            int stockBefore = stockAfter - quantity;
                            movement.setLong(1, productId);
                            movement.setLong(2, operatorId);
                            movement.setLong(3, detailId);
                            movement.setInt(4, quantity);
                            movement.setInt(5, stockBefore);
                            movement.setInt(6, stockAfter);
                            movement.setString(7, note == null || note.isBlank()
                                    ? "Reversal of sale #" + saleId : note.trim());
                            movement.executeUpdate();
                        }
                        if (!foundDetail) throw new IllegalArgumentException("La venta no contiene detalles.");
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(updateStatus)) {
                    statement.setLong(1, saleId);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // Closing the connection below releases it even if reset fails.
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible cancelar la venta en PostgreSQL.", exception);
        }
        return findById(saleId).orElseThrow(() -> new IllegalStateException("La venta cancelada no se encontró."));
    }

    @Override
    public Optional<Venta> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = saleSelect() + " WHERE s.id = ?";
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapSale(connection, result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible consultar la venta en PostgreSQL.", exception);
        }
    }

    @Override
    public List<Venta> findAll() {
        return query(null, null);
    }

    @Override
    public List<Venta> findByPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("El período de fechas no es válido.");
        }
        return query(from, to);
    }

    private List<Venta> query(LocalDate from, LocalDate to) {
        boolean filtered = from != null;
        String sql = saleSelect()
                + (filtered ? " WHERE s.status = 'COMPLETED' AND (s.sale_date AT TIME ZONE 'America/Bogota')::date BETWEEN ? AND ?" : " WHERE s.status = 'COMPLETED'")
                + " ORDER BY s.sale_date, s.id";
        List<Venta> sales = new ArrayList<>();
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (filtered) {
                statement.setObject(1, from);
                statement.setObject(2, to);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) sales.add(mapSale(connection, result));
            }
            return sales;
        } catch (SQLException exception) {
            throw new IllegalStateException("No fue posible listar ventas desde PostgreSQL.", exception);
        }
    }

    private Venta mapSale(Connection connection, ResultSet result) throws SQLException {
        long id = result.getLong("id");
        long customerIdValue = result.getLong("customer_id");
        Long customerId = result.wasNull() ? null : customerIdValue;
        LocalDateTime date = result.getTimestamp("sale_date").toInstant()
                .atZone(BUSINESS_ZONE).toLocalDateTime();
        List<DetalleVenta> details = new ArrayList<>();
        String detailSql = """
                SELECT id, product_id, product_name, category_name, quantity, unit_price
                FROM sale_details WHERE sale_id = ? ORDER BY id
                """;
        try (PreparedStatement statement = connection.prepareStatement(detailSql)) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    details.add(new DetalleVenta(rows.getLong("id"), rows.getLong("product_id"),
                            rows.getString("product_name"), rows.getString("category_name"),
                            rows.getInt("quantity"), rows.getBigDecimal("unit_price").doubleValue()));
                }
            }
        }
        Venta sale = new Venta(id, date, customerId, result.getLong("user_id"),
                result.getLong("payment_method_id"), result.getString("status"), details);
        sale.setClienteNombre(result.getString("customer_name"));
        sale.setMetodoPago(result.getString("payment_method_name"));
        return sale;
    }

    private static String saleSelect() {
        return """
                SELECT s.id, s.customer_id, s.user_id, s.payment_method_id, s.sale_date, s.status,
                       customers.name AS customer_name, payment_methods.name AS payment_method_name
                FROM sales s
                LEFT JOIN customers ON customers.id = s.customer_id
                JOIN payment_methods ON payment_methods.id = s.payment_method_id
                """;
    }

    private static void ensureActiveOperator(Connection connection, Long userId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE id = ? AND is_active = TRUE FOR SHARE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("El usuario de la venta no está activo.");
            }
        }
    }

    private static void ensureActiveCustomer(Connection connection, Long customerId) throws SQLException {
        String sql = "SELECT 1 FROM customers WHERE id = ? AND is_active = TRUE FOR SHARE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("El cliente no existe o está inactivo.");
            }
        }
    }

    private static void ensureActivePaymentMethod(Connection connection, Long paymentMethodId)
            throws SQLException {
        String sql = "SELECT 1 FROM payment_methods WHERE id = ? AND is_active = TRUE FOR SHARE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, paymentMethodId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("El método de pago no está activo.");
            }
        }
    }

    private static void validate(Venta sale) {
        if (sale == null) throw new IllegalArgumentException("La venta es obligatoria.");
        if (sale.getUserId() == null) throw new IllegalArgumentException("El usuario de la venta es obligatorio.");
        if (sale.getPaymentMethodId() == null) {
            throw new IllegalArgumentException("El método de pago es obligatorio.");
        }
        if (sale.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La venta debe contener al menos un producto.");
        }
        for (DetalleVenta detail : sale.getDetalles()) {
            if (detail == null || detail.getProductoId() == null || detail.getCantidad() <= 0) {
                throw new IllegalArgumentException("Cada detalle debe indicar producto y cantidad positiva.");
            }
            if (detail.getProductoNombre() == null || detail.getCategoriaNombre() == null) {
                throw new IllegalArgumentException("Cada detalle debe conservar el nombre del producto y categoría.");
            }
            if (!Double.isFinite(detail.getPrecioUnitario()) || detail.getPrecioUnitario() < 0) {
                throw new IllegalArgumentException("El precio unitario debe ser un número válido y no negativo.");
            }
        }
    }
}
