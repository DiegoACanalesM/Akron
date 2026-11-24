package com.negocio.analitica;

import com.negocio.analitica.controller.*;
import com.negocio.analitica.model.*;
import com.negocio.analitica.repository.MetodoPagoRepository;
import com.negocio.analitica.security.AuthenticatedSession;
import com.negocio.analitica.security.ConsoleAuthenticationFlow;
import com.negocio.analitica.service.UserManagementService;
import com.negocio.analitica.service.PermissionManagementService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    private final MetodoPagoRepository paymentMethods;
    private final InventoryController inventory;

    public ConsoleMenu(Scanner scanner, CategoryController categories, ProductController products,
                       ClienteController clientes, VentaController ventas, AnaliticaController analitica,
                       CsvClienteImporter csvClienteImporter, AuthenticatedSession session,
                       UserManagementService userManagement,
                       PermissionManagementService permissionManagement) {
        this(scanner, categories, products, clientes, ventas, analitica, csvClienteImporter,
                session, userManagement, permissionManagement, null, null);
    }

    public ConsoleMenu(Scanner scanner, CategoryController categories, ProductController products,
                       ClienteController clientes, VentaController ventas, AnaliticaController analitica,
                       CsvClienteImporter csvClienteImporter, AuthenticatedSession session,
                       UserManagementService userManagement,
                       PermissionManagementService permissionManagement,
                       MetodoPagoRepository paymentMethods) {
        this(scanner, categories, products, clientes, ventas, analitica, csvClienteImporter,
                session, userManagement, permissionManagement, paymentMethods, null);
    }

    public ConsoleMenu(Scanner scanner, CategoryController categories, ProductController products,
                       ClienteController clientes, VentaController ventas, AnaliticaController analitica,
                       CsvClienteImporter csvClienteImporter, AuthenticatedSession session,
                       UserManagementService userManagement,
                       PermissionManagementService permissionManagement,
                       MetodoPagoRepository paymentMethods, InventoryController inventory) {
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
        this.paymentMethods = paymentMethods;
        this.inventory = inventory;
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
            if (paymentMethods != null) System.out.println("12. Cancelar una venta");
            if (inventory != null) {
                System.out.println("13. Ajustar existencias");
                System.out.println("14. Consultar historial de inventario");
            }
            System.out.println("15. Editar producto");
            System.out.println("16. Desactivar producto");
            System.out.println("17. Editar cliente");
            System.out.println("18. Desactivar cliente");
            System.out.println("19. Editar categoría");
            System.out.println("20. Desactivar categoría");
            System.out.println("21. Consultar detalle de venta");
            System.out.println("0. Cerrar sesión y salir");
            try {
                int opcion = leerEntero("Seleccione una opción: ");
                switch (opcion) {
                    case 1 -> { require("CATEGORIES_MANAGE"); registrarCategoria(); }
                    case 2 -> { require("PRODUCTS_MANAGE"); registrarProducto(); }
                    case 3 -> { require("CUSTOMERS_MANAGE"); registrarCliente(); }
                    case 4 -> { require("SALES_CREATE"); registrarVenta(); }
                    case 5 -> consultarDatos();
                    case 6 -> { require("ANALYTICS_VIEW"); mostrarIndicadores(); }
                    case 7 -> { require("CUSTOMERS_MANAGE"); importarClientesCsv(); }
                    case 8, 9 -> { require("INVENTORY_MANAGE"); if (opcion == 8) registrarEntradaInventario(); else registrarSalidaInventario(); }
                    case 10 -> { require("INVENTORY_VIEW"); consultarStockBajo(); }
                    case 11 -> { require("USERS_MANAGE"); administrarUsuarios(); }
                    case 12 -> { if (paymentMethods == null) throw new IllegalArgumentException("Opción no disponible."); require("SALES_CANCEL"); cancelarVenta(); }
                    case 13 -> { require("INVENTORY_MANAGE"); requireInventory(); ajustarInventario(); }
                    case 14 -> { require("INVENTORY_VIEW"); requireInventory(); consultarHistorialInventario(); }
                    case 15 -> { require("PRODUCTS_MANAGE"); editarProducto(); }
                    case 16 -> { require("PRODUCTS_MANAGE"); desactivarProducto(); }
                    case 17 -> { require("CUSTOMERS_MANAGE"); editarCliente(); }
                    case 18 -> { require("CUSTOMERS_MANAGE"); desactivarCliente(); }
                    case 19 -> { require("CATEGORIES_MANAGE"); editarCategoria(); }
                    case 20 -> { require("CATEGORIES_MANAGE"); desactivarCategoria(); }
                    case 21 -> { require("SALES_VIEW"); consultarDetalleVenta(); }
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
        System.out.println("1. Listar usuarios");
        System.out.println("2. Crear empleado");
        System.out.println("3. Activar/desactivar cuenta");
        System.out.println("4. Conceder o denegar permiso individual");
        System.out.println("5. Restaurar permiso predeterminado del rol");
        System.out.println("0. Volver");
        int option = leerEntero("Seleccione: ");
        switch (option) {
            case 1 -> userManagement.listUsers(session).forEach(user ->
                    System.out.printf("%d | %s | %s | %s%n", user.getId(), user.getUsername(),
                            user.getFullName(), user.isActive() ? "ACTIVO" : "INACTIVO"));
            case 2 -> {
                String username = leerTexto("Nombre de usuario: ");
                String fullName = leerTexto("Nombre completo: ");
                char[] password = ConsoleAuthenticationFlow.readPassword(
                        scanner, "Contraseña (mínimo 10 caracteres): ");
                userManagement.createEmployee(session, username, fullName, password);
                System.out.println("Empleado creado correctamente.");
            }
            case 3 -> {
                long userId = leerLong("ID del usuario: ");
                boolean active = leerTexto("¿Activar la cuenta? (s/n): ").equalsIgnoreCase("s");
                userManagement.setUserActive(session, userId, active);
                System.out.println("Estado de la cuenta actualizado.");
            }
            case 4, 5 -> {
                long userId = leerLong("ID del empleado: ");
                String permission = leerTexto("Código del permiso (ej. SALES_CREATE): ").toUpperCase();
                if (option == 4) {
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
        List<Category> lista = categories.getAllCategories().stream().filter(Category::isActive).toList();
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
        System.out.println("\n--- PRODUCTOS CON STOCK BAJO ---");
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
        List<Product> listaProductos = products.getAllProducts().stream()
                .filter(Product::isActive).filter(p -> p.getCategory().isActive()).toList();
        if (listaProductos.isEmpty()) {
            System.out.println("Se necesita al menos un producto activo."); return;
        }
        System.out.println("\nClientes (opcional):");
        listaClientes.forEach(c -> System.out.printf("%d - %s (%s)%n", c.getId(), c.getNombre(), c.getDocumento()));
        System.out.println("0 - Venta sin cliente");
        long selectedCustomer = leerLong("ID del cliente (0 si no aplica): ");
        Long clienteId = selectedCustomer == 0 ? null : selectedCustomer;
        if (clienteId != null && listaClientes.stream().noneMatch(c -> c.getId().equals(clienteId))) {
            throw new IllegalArgumentException("El cliente seleccionado no está activo o no existe.");
        }
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
        Venta solicitud;
        if (paymentMethods != null) {
            List<MetodoPago> methods = paymentMethods.findAllActive();
            if (methods.isEmpty()) {
                System.out.println("No hay métodos de pago activos configurados.");
                return;
            }
            System.out.println("\nMétodos de pago:");
            methods.forEach(method -> System.out.printf("%d - %s%n", method.getId(), method.getName()));
            long paymentMethodId = leerLong("ID del método de pago: ");
            solicitud = new Venta(null, null, clienteId, null, paymentMethodId, "DRAFT", detalles);
        } else {
            String methodName = leerTexto("Método de pago: ");
            solicitud = new Venta(null, null, clienteId, null, methodName, detalles);
        }
        Venta registrada = ventas.registrar(solicitud);
        System.out.printf("Venta #%d registrada. Total: $%.2f%n", registrada.getId(), registrada.getTotal());
    }

    private void consultarDetalleVenta() {
        List<Venta> completed = ventas.listar().stream()
                .filter(sale -> "COMPLETED".equals(sale.getStatus()))
                .toList();
        if (completed.isEmpty()) {
            System.out.println("No hay ventas completadas para consultar.");
            return;
        }
        completed.forEach(sale -> System.out.printf("#%d | %s | %s | $%.2f%n",
                sale.getId(), sale.getFecha(),
                sale.getClienteNombre() == null ? "Sin cliente" : sale.getClienteNombre(),
                sale.getTotal()));
        Venta sale = ventas.obtener(leerLong("ID de la venta: "));
        System.out.printf("\n--- DETALLE DE VENTA #%d ---\n", sale.getId());
        System.out.println("Fecha: " + sale.getFecha());
        System.out.println("Cliente: " + (sale.getClienteNombre() == null ? "Sin cliente" : sale.getClienteNombre()));
        System.out.println("Método de pago: " + sale.getMetodoPago());
        System.out.println("Estado: " + sale.getStatus());
        sale.getDetalles().forEach(detail -> System.out.printf(
                "- %s | categoría: %s | cantidad: %d | unitario: $%.2f | subtotal: $%.2f%n",
                detail.getProductoNombre(), detail.getCategoriaNombre(), detail.getCantidad(),
                detail.getPrecioUnitario(), detail.getSubtotal()));
        System.out.printf("TOTAL: $%.2f%n", sale.getTotal());
    }

    private void cancelarVenta() {
        List<Venta> completed = ventas.listar().stream()
                .filter(sale -> "COMPLETED".equals(sale.getStatus()))
                .toList();
        if (completed.isEmpty()) {
            System.out.println("No hay ventas completadas para cancelar.");
            return;
        }
        completed.forEach(sale -> System.out.printf("%d | %s | $%.2f%n",
                sale.getId(), sale.getFecha(), sale.getTotal()));
        long saleId = leerLong("ID de la venta que desea cancelar: ");
        String note = leerTexto("Motivo de la cancelación: ");
        Venta cancelled = ventas.cancelar(saleId, note);
        System.out.println("Venta #" + cancelled.getId() + " cancelada y existencias restauradas.");
    }

    private void consultarDatos() {
        if (session.hasPermission("CATEGORIES_VIEW")) {
            System.out.println("\n--- CATEGORÍAS ---");
            categories.getAllCategories().forEach(c -> System.out.printf("%d | %s | %s | %s%n",
                    c.getId(), c.getName(), c.getDescription(), c.isActive() ? "ACTIVA" : "INACTIVA"));
        }
        if (session.hasPermission("PRODUCTS_VIEW")) {
            System.out.println("\n--- PRODUCTOS ---");
            products.getAllProducts().forEach(p -> System.out.printf("%d | %s | $%.2f | stock %d | mínimo %d | %s%n",
                    p.getId(), p.getName(), p.getPrice(), p.getStock(), p.getMinimumStock(),
                    p.isActive() ? "ACTIVO" : "INACTIVO"));
        }
        if (session.hasPermission("CUSTOMERS_VIEW")) {
            System.out.println("\n--- CLIENTES ACTIVOS ---");
            clientes.listar().forEach(c -> System.out.printf("%d | %s | documento %s | %s%n",
                    c.getId(), c.getNombre(), c.getDocumento(), c.getTelefono()));
        }
        if (session.hasPermission("SALES_VIEW")) {
            System.out.println("\n--- VENTAS ---");
            ventas.listar().forEach(v -> System.out.printf("%d | %s | %s | $%.2f | %s%n",
                    v.getId(), v.getFecha(), v.getClienteNombre() == null ? "Sin cliente" : v.getClienteNombre(),
                    v.getTotal(), v.getStatus()));
        }
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
        long days = ChronoUnit.DAYS.between(desde, hasta) + 1;
        LocalDate previousEnd = desde.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(days - 1);
        double previousTotal = analitica.totalVentas(previousStart, previousEnd);
        double currentTotal = analitica.totalVentas(desde, hasta);
        double variation = analitica.variacionPorcentual(previousTotal, currentTotal);
        System.out.printf("Período anterior (%s a %s): $%.2f%n", previousStart, previousEnd, previousTotal);
        System.out.printf("Variación frente al período anterior: %.2f%%%n", variation);
        System.out.println("Ventas por categoría:");
        analitica.ventasPorCategoria(desde, hasta).forEach((name, total) ->
                System.out.printf("- %s: $%.2f%n", name, total));
        System.out.println("Ventas por método de pago:");
        analitica.ventasPorMetodoPago(desde, hasta).forEach((name, total) ->
                System.out.printf("- %s: $%.2f%n", name, total));
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


    private void requireInventory() {
        if (inventory == null) throw new IllegalArgumentException("El módulo de inventario no está disponible.");
    }

    private void ajustarInventario() {
        long productId = leerLong("ID del producto: ");
        int stock = leerEntero("Nuevo stock físico contado: ");
        String note = leerTexto("Motivo del ajuste: ");
        inventory.adjustStock(productId, stock, note);
        System.out.println("Ajuste registrado en el historial de inventario.");
    }

    private void consultarHistorialInventario() {
        long productId = leerLong("ID del producto: ");
        List<InventoryMovement> movements = inventory.getHistory(productId);
        if (movements.isEmpty()) { System.out.println("No hay movimientos para ese producto."); return; }
        System.out.println("\n--- HISTORIAL DE INVENTARIO ---");
        movements.forEach(m -> System.out.printf(
                "#%d | %s | %s | cantidad: %d | %d -> %d | %s%n",
                m.getId(), m.getCreatedAt(), m.getMovementType(), m.getQuantity(),
                m.getStockBefore(), m.getStockAfter(), m.getNote() == null ? "" : m.getNote()));
    }

    private void editarProducto() {
        List<Product> all = products.getAllProducts();
        all.forEach(p -> System.out.printf("%d | %s | $%.2f | %s%n",
                p.getId(), p.getName(), p.getPrice(), p.isActive() ? "ACTIVO" : "INACTIVO"));
        Product current = products.getProduct(leerLong("ID del producto: "));
        System.out.println("Deje vacío un campo para conservar su valor.");
        System.out.print("Nombre (" + current.getName() + "): ");
        String name = scanner.nextLine().trim();
        System.out.print("Descripción (" + current.getDescription() + "): ");
        String description = scanner.nextLine().trim();
        System.out.print("Precio (" + current.getPrice() + "): ");
        String priceText = scanner.nextLine().trim();
        System.out.print("Stock mínimo (" + current.getMinimumStock() + "): ");
        String minimumText = scanner.nextLine().trim();
        categories.getAllCategories().stream().filter(Category::isActive)
                .forEach(c -> System.out.printf("%d - %s%n", c.getId(), c.getName()));
        System.out.print("ID de categoría (" + current.getCategory().getId() + "): ");
        String categoryText = scanner.nextLine().trim();
        String finalName = name.isEmpty() ? current.getName() : name;
        String finalDescription = description.isEmpty() ? current.getDescription() : description;
        double finalPrice = priceText.isEmpty() ? current.getPrice() : Double.parseDouble(priceText.replace(",", "."));
        int finalMinimum = minimumText.isEmpty() ? current.getMinimumStock() : Integer.parseInt(minimumText);
        Category finalCategory = categoryText.isEmpty() ? current.getCategory()
                : categories.getCategory(Long.parseLong(categoryText));
        products.updateProduct(new Product(current.getId(), finalName, finalDescription, finalPrice,
                current.getStock(), finalMinimum, finalCategory, current.isActive()));
        System.out.println("Producto actualizado. El stock se conserva y solo cambia mediante inventario.");
    }

    private void desactivarProducto() {
        Product current = products.getProduct(leerLong("ID del producto a desactivar: "));
        if (!current.isActive()) { System.out.println("El producto ya está inactivo."); return; }
        current.setActive(false);
        products.updateProduct(current);
        System.out.println("Producto desactivado. Se conserva su historial de ventas e inventario.");
    }

    private void editarCliente() {
        List<Cliente> all = clientes.listar();
        all.forEach(c -> System.out.printf("%d | %s | %s%n", c.getId(), c.getNombre(), c.getDocumento()));
        Cliente current = clientes.obtener(leerLong("ID del cliente: "));
        System.out.println("Deje vacío un campo para conservar su valor.");
        System.out.print("Nombre (" + current.getNombre() + "): ");
        String name = scanner.nextLine().trim();
        System.out.print("Documento (" + current.getDocumento() + "): ");
        String document = scanner.nextLine().trim();
        System.out.print("Teléfono (" + current.getTelefono() + "): ");
        String phone = scanner.nextLine().trim();
        System.out.print("Dirección (" + current.getDireccion() + "): ");
        String address = scanner.nextLine().trim();
        Cliente updated = new Cliente(current.getId(), name.isEmpty() ? current.getNombre() : name,
                document.isEmpty() ? current.getDocumento() : document,
                phone.isEmpty() ? current.getTelefono() : phone,
                address.isEmpty() ? current.getDireccion() : address, current.isActive());
        clientes.actualizar(updated);
        System.out.println("Cliente actualizado. Su historial de ventas permanece intacto.");
    }

    private void desactivarCliente() {
        Cliente current = clientes.obtener(leerLong("ID del cliente a desactivar: "));
        if (!current.isActive()) { System.out.println("El cliente ya está inactivo."); return; }
        clientes.eliminar(current.getId());
        System.out.println("Cliente desactivado. Se conserva su historial de ventas.");
    }

    private void editarCategoria() {
        categories.getAllCategories().forEach(c -> System.out.printf("%d | %s | %s%n",
                c.getId(), c.getName(), c.isActive() ? "ACTIVA" : "INACTIVA"));
        Category current = categories.getCategory(leerLong("ID de la categoría: "));
        System.out.print("Nuevo nombre (" + current.getName() + "): ");
        String name = scanner.nextLine().trim();
        System.out.print("Nueva descripción (" + current.getDescription() + "): ");
        String description = scanner.nextLine().trim();
        categories.updateCategory(new Category(current.getId(),
                name.isEmpty() ? current.getName() : name,
                description.isEmpty() ? current.getDescription() : description, current.isActive()));
        System.out.println("Categoría actualizada.");
    }

    private void desactivarCategoria() {
        Category current = categories.getCategory(leerLong("ID de la categoría a desactivar: "));
        if (!current.isActive()) { System.out.println("La categoría ya está inactiva."); return; }
        current.setActive(false);
        categories.updateCategory(current);
        System.out.println("Categoría desactivada. Los productos y ventas históricos se conservan.");
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
