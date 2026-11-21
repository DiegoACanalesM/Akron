package com.negocio.analitica;

import com.negocio.analitica.controller.*;
import com.negocio.analitica.model.*;
import com.negocio.analitica.repository.sqlite.*;
import com.negocio.analitica.service.*;
import java.nio.file.Path;
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

        System.out.println("Base de datos local lista: data/avnbr.db");
        CsvClienteImporter csvClienteImporter = new CsvClienteImporter(clientes);
        new ConsoleMenu(scanner, categories, products, clientes, ventas, analitica,
                csvClienteImporter).iniciar();
    }
}
