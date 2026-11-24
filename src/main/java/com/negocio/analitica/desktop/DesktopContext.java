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
    private final PostgresPermissionRepository permissionRepository;
    private final PermissionManagementService permissionManagement;
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
        permissionRepository = new PostgresPermissionRepository(connections);
        authentication = new AuthenticationService(new PostgresUsuarioRepository(connections),
                permissionRepository, hasher);
        userManagement = new UserManagementService(userAdministration, hasher);
        permissionManagement = new PermissionManagementService(
                new PostgresPermissionAdministrationRepository(connections));
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

    public List<com.negocio.analitica.model.Usuario> listUsers(AuthenticatedSession actor) {
        return userManagement.listUsers(actor);
    }
    public void createEmployee(AuthenticatedSession actor, String username, String fullName, char[] password) {
        userManagement.createEmployee(actor, username, fullName, password);
    }
    public void setUserActive(AuthenticatedSession actor, Long userId, boolean active) {
        userManagement.setUserActive(actor, userId, active);
    }
    public boolean isEmployee(Long userId) { return userAdministration.isEmployee(userId); }
    public java.util.Set<String> allPermissionCodes() { return permissionRepository.findAllPermissionCodes(); }
    public java.util.Set<String> effectivePermissions(Long userId) {
        return permissionRepository.findEffectivePermissionCodes(userId);
    }
    public void setPermission(AuthenticatedSession actor, Long userId, String code, boolean granted) {
        permissionManagement.grantOrDeny(actor, userId, code, granted);
    }
    public void restorePermission(AuthenticatedSession actor, Long userId, String code) {
        permissionManagement.restoreRoleDefault(actor, userId, code);
    }

    private static <T> T require(T service) {
        if (service == null) throw new IllegalStateException("Inicia sesión antes de acceder a los módulos.");
        return service;
    }
}
