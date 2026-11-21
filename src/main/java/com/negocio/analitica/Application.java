package com.negocio.analitica;

import com.negocio.analitica.controller.*;
import com.negocio.analitica.database.PostgresConnectionProvider;
import com.negocio.analitica.repository.postgresql.*;
import com.negocio.analitica.security.AuthenticatedSession;
import com.negocio.analitica.security.ConsoleAuthenticationFlow;
import com.negocio.analitica.security.PasswordHasher;
import com.negocio.analitica.security.BCryptPasswordHasher;
import com.negocio.analitica.service.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;

/** Punto de entrada de AKRON. Solo permite una instancia local activa. */
public final class Application {
    private Application() { }

    public static void main(String[] args) {
        try (SingleInstanceGuard ignored =
                     SingleInstanceGuard.acquire(Path.of("data", "akron.lock"));
             Scanner scanner = new Scanner(System.in)) {
            runApplication(scanner);
        } catch (RuntimeException error) {
            System.err.println("No se pudo iniciar AKRON: " + error.getMessage());
            System.exit(1);
        }
    }

    private static void runApplication(Scanner scanner) {
        PostgresConnectionProvider connections = PostgresConnectionProvider.fromEnvironment();
        Optional<AuthenticatedSession> authenticated =
                ConsoleAuthenticationFlow.authenticate(scanner, connections);
        if (authenticated.isEmpty()) {
            System.out.println("AKRON se cerró sin iniciar una sesión.");
            return;
        }

        AuthenticatedSession session = authenticated.get();

        // All commercial modules now use the same local PostgreSQL database.
        PostgresCategoryRepository categoryRepository = new PostgresCategoryRepository(connections);
        PostgresProductRepository productRepository = new PostgresProductRepository(connections);
        PostgresClienteRepository clienteRepository = new PostgresClienteRepository(connections);
        PostgresMetodoPagoRepository paymentMethodRepository = new PostgresMetodoPagoRepository(connections);
        PostgresVentaRepository ventaRepository = new PostgresVentaRepository(connections);
        PostgresInventoryRepository inventoryRepository = new PostgresInventoryRepository(connections);

        CategoryService categoryService = new CategoryService(categoryRepository);
        PostgresInventoryService inventoryService = new PostgresInventoryService(inventoryRepository, session);
        ProductService productService = new ProductService(
                productRepository, categoryRepository, inventoryService);
        ClienteService clienteService = new ClienteService(clienteRepository);
        PostgresVentaService ventaService = new PostgresVentaService(
                clienteService, productService, paymentMethodRepository, ventaRepository, session);

        CategoryController categories = new CategoryController(categoryService);
        ProductController products = new ProductController(productService);
        ClienteController clientes = new ClienteController(clienteService);
        VentaController ventas = new VentaController(ventaService);
        AnaliticaController analitica = new AnaliticaController(
                new AnaliticaService(ventaRepository, productService));

        PasswordHasher hasher = new BCryptPasswordHasher();
        UserManagementService userManagement = new UserManagementService(
                new PostgresUserAdministrationRepository(connections), hasher);
        PermissionManagementService permissionManagement = new PermissionManagementService(
                new PostgresPermissionAdministrationRepository(connections));

        System.out.println("Autenticación, usuarios y módulos comerciales: PostgreSQL.");
        System.out.println("Ventas e inventario comparten transacciones en la misma base de datos.");

        CsvClienteImporter csvClienteImporter = new CsvClienteImporter(clientes);
        new ConsoleMenu(scanner, categories, products, clientes, ventas, analitica,
                csvClienteImporter, session, userManagement, permissionManagement,
                paymentMethodRepository).iniciar();
    }
}
