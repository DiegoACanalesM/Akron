package com.negocio.analitica.desktop;

import com.negocio.analitica.controller.*;
import com.negocio.analitica.database.PostgresConnectionProvider;
import com.negocio.analitica.repository.postgresql.*;
import com.negocio.analitica.security.*;
import com.negocio.analitica.service.*;
import com.negocio.analitica.model.MetodoPago;
import java.util.List;

/** Builds shared application services and binds permission-sensitive services to a logged-in session. */
public final class DesktopContext {
    private final PostgresConnectionProvider connections;
    private final PostgresUserAdministrationRepository userAdministration;
    private final AuthenticationService authentication;
    private final UserManagementService userManagement;
    private CategoryController categories;
    private ProductController products;
    private ClienteController clients;
    private VentaController sales;
    private InventoryController inventory;
    private AnaliticaController analytics;
    private PostgresMetodoPagoRepository paymentMethods;

    public DesktopContext() {
        connections = PostgresConnectionProvider.fromEnvironment();
        userAdministration = new PostgresUserAdministrationRepository(connections);
        PasswordHasher hasher = new BCryptPasswordHasher();
        authentication = new AuthenticationService(new PostgresUsuarioRepository(connections),
                new PostgresPermissionRepository(connections), hasher);
        userManagement = new UserManagementService(userAdministration, hasher);
    }

    public boolean needsOwnerSetup() { return !userAdministration.hasAnyUser(); }
    public void createOwner(String username, String fullName, char[] password) {
        userManagement.bootstrapOwner(username, fullName, password);
    }
    public AuthenticatedSession login(String username, char[] password) {
        return authentication.login(username, password).orElse(null);
    }

    /** Creates operator-scoped services only after credentials and permissions are verified. */
    public void activateSession(AuthenticatedSession session) {
        if (session == null) throw new IllegalArgumentException("An authenticated session is required.");
        var categoryRepository = new PostgresCategoryRepository(connections);
        var productRepository = new PostgresProductRepository(connections);
        var clientRepository = new PostgresClienteRepository(connections);
        paymentMethods = new PostgresMetodoPagoRepository(connections);
        var paymentRepository = paymentMethods;
        var saleRepository = new PostgresVentaRepository(connections);
        var inventoryRepository = new PostgresInventoryRepository(connections);

        var categoryService = new CategoryService(categoryRepository);
        var inventoryService = new PostgresInventoryService(inventoryRepository, session);
        var productService = new ProductService(productRepository, categoryRepository, inventoryService);
        var clientService = new ClienteService(clientRepository);
        var saleService = new PostgresVentaService(clientService, productService,
                paymentRepository, saleRepository, session);

        categories = new CategoryController(categoryService);
        products = new ProductController(productService);
        clients = new ClienteController(clientService);
        sales = new VentaController(saleService);
        inventory = new InventoryController(inventoryService);
        analytics = new AnaliticaController(new AnaliticaService(saleRepository, productService));
    }

    public CategoryController categories() { return require(categories); }
    public ProductController products() { return require(products); }
    public ClienteController clients() { return require(clients); }
    public VentaController sales() { return require(sales); }
    public InventoryController inventory() { return require(inventory); }
    public AnaliticaController analytics() { return require(analytics); }
    public List<MetodoPago> getActivePaymentMethods() { return require(paymentMethods).findAllActive(); }

    private static <T> T require(T service) {
        if (service == null) throw new IllegalStateException("Inicia sesión antes de acceder a los módulos.");
        return service;
    }
}
