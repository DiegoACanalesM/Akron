package com.negocio.analitica;

import com.negocio.analitica.controller.*;
import com.negocio.analitica.model.*;
import com.negocio.analitica.security.AuthenticatedSession;
import com.negocio.analitica.security.ConsoleAuthenticationFlow;
import com.negocio.analitica.service.UserManagementService;
import com.negocio.analitica.service.PermissionManagementService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Interfaz de consola para captura manual e importación CSV.
 * El menú delega las reglas de negocio en los controladores existentes.
 */
public final class ConsoleMenu {
    private final Scanner scanner;
    private final CategoryController categories;
    private final ProductController products;
    private final ClienteController clientes;
    private final VentaController ventas;
    private final AnaliticaController analitica;
    private final CsvClienteImporter csvClienteImporter;
    private final AuthenticatedSession session;
    private final UserManagementService userManagement;
    private final PermissionManagementService permissionManagement;

    public ConsoleMenu(Scanner scanner, CategoryController categories, ProductController products,
                       ClienteController clientes, VentaController ventas, AnaliticaController analitica,
                       CsvClienteImporter csvClienteImporter, AuthenticatedSession session,
                       UserManagementService userManagement,
                       PermissionManagementService permissionManagement) {
        this.scanner = scanner;
        this.categories = categories;
        this.products = products;
        this.clientes = clientes;
        this.ventas = ventas;
        this.analitica = analitica;
        this.csvClienteImporter = csvClienteImporter;
        this.session = java.util.Objects.requireNonNull(session);
        this.userManagement = java.util.Objects.requireNonNull(userManagement);
        this.permissionManagement = java.util.Objects.requireNonNull(permissionManagement);
    }

    public void iniciar() {
        boolean continuar = true;
        while (continuar) {
            System.out.println("\n========== AKRON | MENÚ PRINCIPAL ==========");
            System.out.println("Usuario: " + session.getFullName());
            System.out.println("1. Registrar categoría");
            System.out.println("2. Registrar producto");
            System.out.println("3. Registrar cliente");
            System.out.println("4. Registrar venta");
            System.out.println("5. Consultar información almacenada");
            System.out.println("6. Ver indicadores de ventas");
            System.out.println("7. Importar clientes desde archivo CSV");
            System.out.println("8. Registrar entrada de inventario");
            System.out.println("9. Registrar salida de inventario");
            System.out.println("10. Consultar productos con stock bajo");
            System.out.println("11. Administrar usuarios y permisos");
            System.out.println("0. Cerrar sesión y salir");
            try {
                int opcion = leerEntero("Seleccione una opción: ");
                switch (opcion) {
                    case 1 -> { require("CATEGORIES_MANAGE"); registrarCategoria(); }
                    case 2 -> { require("PRODUCTS_MANAGE"); registrarProducto(); }
                    case 3 -> { require("CUSTOMERS_MANAGE"); registrarCliente(); }
                    case 4 -> { require("SALES_CREATE"); registrarVenta(); }
                    case 5 -> { requireAll("CATEGORIES_VIEW", "PRODUCTS_VIEW", "CUSTOMERS_VIEW", "SALES_VIEW"); consultarDatos(); }
                    case 6 -> { require("ANALYTICS_VIEW"); mostrarIndicadores(); }
                    case 7 -> { require("CUSTOMERS_MANAGE"); importarClientesCsv(); }
                    case 8, 9 -> { require("INVENTORY_MANAGE"); if (opcion == 8) registrarEntradaInventario(); else registrarSalidaInventario(); }
                    case 10 -> { require("INVENTORY_VIEW"); consultarStockBajo(); }
                    case 11 -> { require("USERS_MANAGE"); administrarUsuarios(); }
                    case 0 -> { continuar = false; System.out.println("Sesión finalizada. Los datos permanecen guardados."); }
                    default -> System.out.println("Opción no válida.");
                }
            } catch (RuntimeException ex) {
                System.out.println("No fue posible completar la operación: " + ex.getMessage());
            }
        }
    }

    private void administrarUsuarios() {
        System.out.println("\n--- ADMINISTRACIÓN DE USUARIOS ---");
        System.out.println("1. Crear empleado");
        System.out.println("2. Activar/desactivar cuenta");
        System.out.println("3. Conceder o denegar permiso individual");
        System.out.println("4. Restaurar permiso predeterminado del rol");
        System.out.println("0. Volver");
        int option = leerEntero("Seleccione: ");
        switch (option) {
            case 1 -> {
                String username = leerTexto("Nombre de usuario: ");
                String fullName = leerTexto("Nombre completo: ");
                char[] password = ConsoleAuthenticationFlow.readPassword(
                        scanner, "Contraseña (mínimo 10 caracteres): ");
                userManagement.createEmployee(session, username, fullName, password);
                System.out.println("Empleado creado correctamente.");
            }
            case 2 -> {
                long userId = leerLong("ID del usuario: ");
                boolean active = leerTexto("¿Activar la cuenta? (s/n): ").equalsIgnoreCase("s");
                userManagement.setUserActive(session, userId, active);
                System.out.println("Estado de la cuenta actualizado.");
            }
            case 3, 4 -> {
                long userId = leerLong("ID del empleado: ");
                String permission = leerTexto("Código del permiso (ej. SALES_CREATE): ").toUpperCase();
                if (option == 3) {
                    boolean granted = leerTexto("¿Conceder permiso? (s/n; n = denegar): ").equalsIgnoreCase("s");
                    permissionManagement.grantOrDeny(session, userId, permission, granted);
                    System.out.println("Permiso actualizado.");
                } else {
                    permissionManagement.restoreRoleDefault(session, userId, permission);
                    System.out.println("Se restauró el permiso definido por el rol.");
                }
            }
            case 0 -> { return; }
            default -> System.out.println("Opción no válida.");
        }
    }

    private void require(String permission) {
        session.requirePermission(permission);
    }

    private void requireAll(String... permissions) {
        for (String permission : permissions) session.requirePermission(permission);
    }

    private void registrarCategoria() {
        String nombre = leerTexto("Nombre de la categoría: ");
        String descripcion = leerTexto("Descripción: ");
        Category creada = categories.createCategory(new Category(null, nombre, descripcion));
        System.out.println("Categoría guardada con ID " + creada.getId());
    }

    private void registrarProducto() {
        List<Category> lista = categories.getAllCategories();
        if (lista.isEmpty()) { System.out.println("Primero debe registrar una categoría."); return; }
        System.out.println("\nCategorías disponibles:");
        lista.forEach(c -> System.out.printf("%d - %s%n", c.getId(), c.getName()));
        long categoryId = leerLong("ID de categoría: ");
        Category category = categories.getCategory(categoryId);
        String nombre = leerTexto("Nombre del producto: ");
        double precio = leerDouble("Precio: ");
        int stock = leerEntero("Existencias iniciales: ");
        int minimo = leerEntero("Stock mínimo: ");
        Product creado = products.createProduct(new Product(null, nombre, precio, stock, minimo, category));
        System.out.println("Producto guardado con ID " + creado.getId());
    }

    private void registrarEntradaInventario() {
        long productId = leerLong("ID del producto: ");
        int quantity = leerEntero("Cantidad que desea agregar: ");
        Product updated = products.registerStockEntry(productId, quantity);
        System.out.printf("Entrada registrada. Stock actual de %s: %d%n",
                updated.getName(), updated.getStock());
    }

    private void registrarSalidaInventario() {
        long productId = leerLong("ID del producto: ");
        int quantity = leerEntero("Cantidad que desea retirar: ");
        Product updated = products.registerStockExit(productId, quantity);
        System.out.printf("Salida registrada. Stock actual de %s: %d%n",
                updated.getName(), updated.getStock());
    }

    private void consultarStockBajo() {
        List<Product> lowStock = products.getLowStockProducts();
        if (lowStock.isEmpty()) {
            System.out.println("No hay productos con stock bajo.");
            return;
        }
        System.out.println("\\n--- PRODUCTOS CON STOCK BAJO ---");
        lowStock.forEach(product -> System.out.printf(
                "%d | %s | stock: %d | mínimo: %d%n",
                product.getId(), product.getName(),
                product.getStock(), product.getMinimumStock()));
    }

    private void registrarCliente() {
        Cliente cliente = new Cliente(null, leerTexto("Nombre: "), leerTexto("Documento: "),
                leerTexto("Teléfono: "), leerTexto("Dirección: "));
        Cliente creado = clientes.crear(cliente);
        System.out.println("Cliente guardado con ID " + creado.getId());
    }

    private void registrarVenta() {
        List<Cliente> listaClientes = clientes.listar();
        List<Product> listaProductos = products.getAllProducts();
        if (listaClientes.isEmpty() || listaProductos.isEmpty()) {
            System.out.println("Se necesita al menos un cliente y un producto registrado."); return;
        }
        System.out.println("\nClientes:");
        listaClientes.forEach(c -> System.out.printf("%d - %s (%s)%n", c.getId(), c.getNombre(), c.getDocumento()));
        long clienteId = leerLong("ID del cliente: ");
        System.out.println("\nProductos:");
        listaProductos.forEach(p -> System.out.printf("%d - %s | $%.2f | stock: %d%n",
                p.getId(), p.getName(), p.getPrice(), p.getStock()));
        List<DetalleVenta> detalles = new ArrayList<>();
        boolean agregar = true;
        while (agregar) {
            long productId = leerLong("ID del producto: ");
            int cantidad = leerEntero("Cantidad: ");
            detalles.add(new DetalleVenta(null, productId, null, null, cantidad, 0));
            agregar = leerTexto("¿Agregar otro producto? (s/n): ").equalsIgnoreCase("s");
        }
        String metodo = leerTexto("Método de pago: ");
        Venta registrada = ventas.registrar(new Venta(null, null, clienteId, null, metodo, detalles));
        System.out.printf("Venta #%d registrada. Total: $%.2f%n", registrada.getId(), registrada.getTotal());
    }

    private void consultarDatos() {
        System.out.println("\n--- CATEGORÍAS ---");
        categories.getAllCategories().forEach(c -> System.out.printf("%d | %s | %s%n", c.getId(), c.getName(), c.getDescription()));
        System.out.println("\n--- PRODUCTOS ---");
        products.getAllProducts().forEach(p -> System.out.printf("%d | %s | $%.2f | stock %d | mínimo %d%n",
                p.getId(), p.getName(), p.getPrice(), p.getStock(), p.getMinimumStock()));
        System.out.println("\n--- CLIENTES ---");
        clientes.listar().forEach(c -> System.out.printf("%d | %s | documento %s | %s%n",
                c.getId(), c.getNombre(), c.getDocumento(), c.getTelefono()));
        System.out.println("\n--- VENTAS ---");
        ventas.listar().forEach(v -> System.out.printf("%d | %s | %s | $%.2f%n",
                v.getId(), v.getFecha(), v.getClienteNombre(), v.getTotal()));
    }

    private void mostrarIndicadores() {
        LocalDate desde = LocalDate.parse(leerTexto("Fecha inicial (AAAA-MM-DD): "));
        LocalDate hasta = LocalDate.parse(leerTexto("Fecha final (AAAA-MM-DD): "));
        if (hasta.isBefore(desde)) throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial.");
        System.out.printf("Ingresos: $%.2f%n", analitica.totalVentas(desde, hasta));
        System.out.println("Transacciones: " + analitica.cantidadTransacciones(desde, hasta));
        System.out.println("Unidades vendidas: " + analitica.unidadesVendidas(desde, hasta));
        System.out.printf("Ticket promedio: $%.2f%n", analitica.ticketPromedio(desde, hasta));
        System.out.println("Producto más vendido: " + analitica.productoMasVendido(desde, hasta).orElse("Sin ventas"));
        System.out.println("Productos con stock bajo:");
        analitica.productosConStockBajo().forEach(p ->
                System.out.printf("- %s: %d unidades (mínimo %d)%n", p.getName(), p.getStock(), p.getMinimumStock()));
    }

    /** La lectura del archivo está delegada al importador especializado. */
    private void importarClientesCsv() {
        String ruta = leerTexto("Ruta del archivo CSV: ");
        CsvClienteImporter.ResultadoImportacion resultado =
                csvClienteImporter.importar(java.nio.file.Path.of(ruta));
        System.out.printf("Importación terminada. Guardados: %d | Rechazados: %d%n",
                resultado.importados(), resultado.rechazados());
    }

    private String leerTexto(String mensaje) {
        System.out.print(mensaje);
        String valor = scanner.nextLine().trim();
        if (valor.isEmpty()) throw new IllegalArgumentException("El campo no puede estar vacío.");
        return valor;
    }
    private int leerEntero(String mensaje) {
        try { return Integer.parseInt(leerTexto(mensaje)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Ingrese un número entero válido."); }
    }
    private long leerLong(String mensaje) {
        try { return Long.parseLong(leerTexto(mensaje)); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Ingrese un ID numérico válido."); }
    }
    private double leerDouble(String mensaje) {
        try { return Double.parseDouble(leerTexto(mensaje).replace(",", ".")); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Ingrese un valor numérico válido."); }
    }
}
