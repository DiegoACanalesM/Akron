package com.negocio.analitica;

import com.negocio.analitica.controller.*;
import com.negocio.analitica.database.PostgresConnectionProvider;
import com.negocio.analitica.model.*;
import com.negocio.analitica.repository.postgresql.PostgresPermissionAdministrationRepository;
import com.negocio.analitica.repository.postgresql.PostgresUserAdministrationRepository;
import com.negocio.analitica.repository.sqlite.*;
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
        } catch (IllegalStateException error) {
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }

    private static void runApplication(Scanner scanner) {
        // Authentication and user permissions are stored in PostgreSQL.
        PostgresConnectionProvider connections = PostgresConnectionProvider.fromEnvironment();
        Optional<AuthenticatedSession> authenticated =
                ConsoleAuthenticationFlow.authenticate(scanner, connections);
        if (authenticated.isEmpty()) {
            System.out.println("AKRON se cerró sin iniciar una sesión.");
            return;
        }

        // Business repositories remain on SQLite until their PostgreSQL migration is completed.
        SqliteDatabase database = new SqliteDatabase("jdbc:sqlite:data/avnbr.db");
        SqliteCategoryRepository categoryRepository = new SqliteCategoryRepository(database);
        SqliteProductRepository productRepository = new SqliteProductRepository(database);
        SqliteClienteRepository clienteRepository = new SqliteClienteRepository(database);
        SqliteVentaRepository ventaRepository = new SqliteVentaRepository(database);

        CategoryService categoryService = new CategoryService(categoryRepository);
        ProductService productService = new ProductService(productRepository, categoryRepository);
        ClienteService clienteService = new ClienteService(clienteRepository);

        CategoryController categories = new CategoryController(categoryService);
        ProductController products = new ProductController(productService);
        ClienteController clientes = new ClienteController(clienteService);
        VentaController ventas = new VentaController(
                new VentaService(clienteService, productService, ventaRepository));
        AnaliticaController analitica = new AnaliticaController(
                new AnaliticaService(ventaRepository, productService));

        PasswordHasher hasher = new BCryptPasswordHasher();
        UserManagementService userManagement = new UserManagementService(
                new PostgresUserAdministrationRepository(connections), hasher);
        PermissionManagementService permissionManagement = new PermissionManagementService(
                new PostgresPermissionAdministrationRepository(connections));

        System.out.println("Autenticación: PostgreSQL.");
        System.out.println("Datos comerciales: SQLite (migración PostgreSQL pendiente).");
        CsvClienteImporter csvClienteImporter = new CsvClienteImporter(clientes);
        new ConsoleMenu(scanner, categories, products, clientes, ventas, analitica,
                csvClienteImporter, authenticated.get(), userManagement, permissionManagement).iniciar();
    }
}
