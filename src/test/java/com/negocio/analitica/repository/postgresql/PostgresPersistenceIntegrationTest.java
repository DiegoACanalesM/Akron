package com.negocio.analitica.repository.postgresql;

import com.negocio.analitica.database.ConnectionProvider;
import com.negocio.analitica.model.DetalleVenta;
import com.negocio.analitica.model.Venta;
import com.negocio.analitica.repository.postgresql.PostgresClienteRepository;
import com.negocio.analitica.repository.postgresql.PostgresInventoryRepository;
import com.negocio.analitica.repository.postgresql.PostgresMetodoPagoRepository;
import com.negocio.analitica.repository.postgresql.PostgresVentaRepository;
import com.negocio.analitica.model.Cliente;
import com.negocio.analitica.model.InventoryMovement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Live PostgreSQL tests. Set AKRON_TEST_DB_URL, AKRON_TEST_DB_USER and
 * AKRON_TEST_DB_PASSWORD and apply database/02..05 scripts before running.
 */
class PostgresPersistenceIntegrationTest {
    private String url;
    private String user;
    private String password;
    private ConnectionProvider connections;
    private Long userId;
    private Long categoryId;
    private Long productId;
    private Long paymentMethodId;
    private Long customerId;
    private String suffix;

    @BeforeEach
    void setUp() throws SQLException {
        url = System.getenv("AKRON_TEST_DB_URL");
        Assumptions.assumeTrue(url != null && !url.isBlank(),
                "Live PostgreSQL tests are skipped unless AKRON_TEST_DB_URL is configured.");
        user = System.getenv().getOrDefault("AKRON_TEST_DB_USER", "postgres");
        password = System.getenv().getOrDefault("AKRON_TEST_DB_PASSWORD", "");
        connections = () -> DriverManager.getConnection(url, user, password);
        suffix = UUID.randomUUID().toString().replace("-", "");

        try (Connection connection = connections.getConnection()) {
            userId = insertId(connection,
                    "INSERT INTO users(username,password_hash,full_name) VALUES (?,?,?) RETURNING id",
                    "it_" + suffix, "integration-test-hash", "Integration Test");
            categoryId = insertId(connection,
                    "INSERT INTO categories(name,description) VALUES (?,?) RETURNING id",
                    "IT Category " + suffix, "Temporary integration category");
            productId = insertId(connection,
                    "INSERT INTO products(name,description,price,stock,minimum_stock,category_id) "
                            + "VALUES (?,?,?,?,?,?) RETURNING id",
                    "IT Product " + suffix, "Temporary integration product",
                    new java.math.BigDecimal("9.99"), 10, 2, categoryId);
            paymentMethodId = insertId(connection,
                    "INSERT INTO payment_methods(name) VALUES (?) RETURNING id",
                    "IT Payment " + suffix);
            customerId = insertId(connection,
                    "INSERT INTO customers(name,document) VALUES (?,?) RETURNING id",
                    "IT Customer " + suffix, "DOC-" + suffix);
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connections == null) return;
        try (Connection connection = connections.getConnection()) {
            if (productId != null) {
                execute(connection, "DELETE FROM inventory_movements WHERE product_id = ?", productId);
            }
            if (userId != null) {
                execute(connection, "DELETE FROM sale_details WHERE sale_id IN (SELECT id FROM sales WHERE user_id = ?)", userId);
                execute(connection, "DELETE FROM sales WHERE user_id = ?", userId);
            }
            if (productId != null) execute(connection, "DELETE FROM products WHERE id = ?", productId);
            if (categoryId != null) execute(connection, "DELETE FROM categories WHERE id = ?", categoryId);
            if (customerId != null) execute(connection, "DELETE FROM customers WHERE id = ?", customerId);
            if (paymentMethodId != null) execute(connection, "DELETE FROM payment_methods WHERE id = ?", paymentMethodId);
            if (userId != null) execute(connection, "DELETE FROM users WHERE id = ?", userId);
        }
    }

    @Test
    void saleAndCancellationAreAtomicWithStockAndMovementHistory() throws SQLException {
        PostgresVentaRepository sales = new PostgresVentaRepository(connections);
        Venta saved = sales.save(newSale(3, customerId));

        assertNotNull(saved.getId());
        assertEquals("COMPLETED", saved.getStatus());
        assertEquals(7, readStock());

        List<InventoryMovement> afterSale =
                new PostgresInventoryRepository(connections).findByProductId(productId);
        assertEquals(1, afterSale.size());
        assertEquals("SALE", afterSale.get(0).getMovementType().name());

        Venta cancelled = sales.cancelCompletedSale(saved.getId(), userId, "Integration cancellation");
        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals(10, readStock());

        List<InventoryMovement> afterCancellation =
                new PostgresInventoryRepository(connections).findByProductId(productId);
        assertEquals(2, afterCancellation.size());
        assertTrue(afterCancellation.stream().anyMatch(m -> "SALE_REVERSAL".equals(m.getMovementType().name())));
    }

    @Test
    void insufficientStockRollsBackSaleAndMovement() throws SQLException {
        PostgresVentaRepository sales = new PostgresVentaRepository(connections);
        assertThrows(IllegalArgumentException.class, () -> sales.save(newSale(11, null)));

        assertEquals(10, readStock());
        assertEquals(0, count("SELECT COUNT(*) FROM sales WHERE user_id = ?", userId));
        assertEquals(0, count("SELECT COUNT(*) FROM inventory_movements WHERE product_id = ?", productId));
    }

    @Test
    void inventoryEntryExitAndAdjustmentUpdateStockAndAppendHistory() throws SQLException {
        PostgresInventoryRepository inventory = new PostgresInventoryRepository(connections);
        inventory.registerEntry(productId, userId, 3, "Received stock");
        inventory.registerExit(productId, userId, 2, "Damaged stock");
        inventory.adjustStock(productId, userId, 4, "Physical count");

        assertEquals(4, readStock());
        assertEquals(3, inventory.findByProductId(productId).size());
    }

    @Test
    void inactiveCategoryBlocksSalesButKeepsInventoryManageable() throws SQLException {
        try (Connection connection = connections.getConnection()) {
            execute(connection, "UPDATE categories SET is_active = FALSE WHERE id = ?", categoryId);
        }

        PostgresInventoryRepository inventory = new PostgresInventoryRepository(connections);
        inventory.registerEntry(productId, userId, 2, "Inventory after category deactivation");
        inventory.registerExit(productId, userId, 1, "Inventory correction");
        inventory.adjustStock(productId, userId, 8, "Physical count");

        assertEquals(8, readStock());
        assertEquals(3, inventory.findByProductId(productId).size());

        PostgresVentaRepository sales = new PostgresVentaRepository(connections);
        assertThrows(IllegalArgumentException.class, () -> sales.save(newSale(1, null)));
        assertEquals(8, readStock());
        assertEquals(0, count("SELECT COUNT(*) FROM sales WHERE user_id = ?", userId));
    }

    @Test
    void inactiveCustomerCannotBeUsedForANewSale() throws SQLException {
        try (Connection connection = connections.getConnection()) {
            execute(connection, "UPDATE customers SET is_active = FALSE WHERE id = ?", customerId);
        }
        PostgresVentaRepository sales = new PostgresVentaRepository(connections);
        assertThrows(IllegalArgumentException.class, () -> sales.save(newSale(1, customerId)));
        assertEquals(10, readStock());
        assertEquals(0, count("SELECT COUNT(*) FROM sales WHERE user_id = ?", userId));
    }

    @Test
    void customerRepositorySupportsOptionalDocumentsAndLookup() {
        PostgresClienteRepository customers = new PostgresClienteRepository(connections);
        Cliente noDocument = customers.save(new Cliente(null, "Walk-in " + suffix, null, null, null));
        assertNotNull(noDocument.getId());
        assertEquals(noDocument.getId(), customers.findById(noDocument.getId()).orElseThrow().getId());
        assertTrue(customers.findByDocumento(" ").isEmpty());
        customers.deleteById(noDocument.getId());
    }

    private Venta newSale(int quantity, Long selectedCustomerId) {
        DetalleVenta detail = new DetalleVenta(null, productId,
                "IT Product " + suffix, "IT Category " + suffix, quantity, 9.99);
        return new Venta(null, null, selectedCustomerId, userId,
                paymentMethodId, "COMPLETED", List.of(detail));
    }

    private int readStock() throws SQLException {
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT stock FROM products WHERE id = ?")) {
            statement.setLong(1, productId);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getInt(1);
            }
        }
    }

    private long count(String sql, long parameter) throws SQLException {
        try (Connection connection = connections.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, parameter);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getLong(1);
            }
        }
    }

    private static long insertId(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getLong(1);
            }
        }
    }

    private static void execute(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
        }
    }
}
