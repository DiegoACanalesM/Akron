package com.negocio.analitica.desktop;

import com.negocio.analitica.controller.*;
import com.negocio.analitica.database.PostgresConnectionProvider;
import com.negocio.analitica.repository.postgresql.*;
import com.negocio.analitica.security.*;
import com.negocio.analitica.service.*;

/** Builds the application's existing services once and shares them with JavaFX views. */
public final class DesktopContext {
    private final PostgresConnectionProvider connections;
    private final PostgresUserAdministrationRepository userAdministration;
    private final AuthenticationService authentication;
    private final UserManagementService userManagement;
    private final CategoryController categories;
    private final ProductController products;
    private final ClienteController clients;
    private final VentaController sales;
    private final InventoryController inventory;
    private final AnaliticaController analytics;

    public DesktopContext() {
        connections = PostgresConnectionProvider.fromEnvironment();
        userAdministration = new PostgresUserAdministrationRepository(connections);
        PasswordHasher hasher = new BCryptPasswordHasher();
        authentication = new AuthenticationService(
                new PostgresUsuarioRepository(connections),
                new PostgresPermissionRepository(connections), hasher);
        userManagement = new UserManagementService(userAdministration, hasher);

        var categoryRepository = new PostgresCategoryRepository(connections);
        var productRepository = new PostgresProductRepository(connections);
        var clientRepository = new PostgresClienteRepository(connections);
        var paymentRepository = new PostgresMetodoPagoRepository(connections);
        var saleRepository = new PostgresVentaRepository(connections);
        var inventoryRepository = new PostgresInventoryRepository(connections);

        var categoryService = new CategoryService(categoryRepository);
        var inventoryService = new PostgresInventoryService(inventoryRepository, null);
        var productService = new ProductService(productRepository, categoryRepository, inventoryService);
        var clientService = new ClienteService(clientRepository);
        var saleService = new PostgresVentaService(clientService, productService,
                paymentRepository, saleRepository, null);

        categories = new CategoryController(categoryService);
        products = new ProductController(productService);
        clients = new ClienteController(clientService);
        sales = new VentaController(saleService);
        inventory = new InventoryController(inventoryService);
        analytics = new AnaliticaController(new AnaliticaService(saleRepository, productService));
    }

    public boolean needsOwnerSetup() { return !userAdministration.hasAnyUser(); }
    public void createOwner(String username, String fullName, char[] password) {
        userManagement.bootstrapOwner(username, fullName, password);
    }
    public AuthenticatedSession login(String username, char[] password) {
        return authentication.login(username, password).orElse(null);
    }
    public CategoryController categories() { return categories; }
    public ProductController products() { return products; }
    public ClienteController clients() { return clients; }
    public VentaController sales() { return sales; }
    public InventoryController inventory() { return inventory; }
    public AnaliticaController analytics() { return analytics; }
}
