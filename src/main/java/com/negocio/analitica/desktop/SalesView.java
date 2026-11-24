package com.negocio.analitica.desktop;

import com.negocio.analitica.model.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Sales workspace backed by the existing transactional PostgreSQL service. */
public final class SalesView extends VBox {
    private final DesktopContext context;
    private final TableView<Venta> salesTable = new TableView<>();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public SalesView(DesktopContext context) {
        this.context = context;
        setSpacing(18); setPadding(new Insets(34));
        Label title = new Label("Ventas"); title.getStyleClass().add("page-title");
        Label subtitle = new Label("Registra ventas y consulta las operaciones realizadas.");
        subtitle.getStyleClass().add("page-subtitle");
        Button create = new Button("Nueva venta"); create.getStyleClass().add("primary-button");
        create.setOnAction(event -> openSaleDialog());
        HBox heading = new HBox(16, title, new Region(), create);
        HBox.setHgrow(heading.getChildren().get(1), Priority.ALWAYS);
        heading.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        salesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        salesTable.getStyleClass().add("data-table");
        salesTable.getColumns().addAll(
                column("N.º", s -> String.valueOf(s.getId())),
                column("Fecha", s -> s.getFecha() == null ? "—" : s.getFecha().format(DATE)),
                column("Cliente", s -> s.getClienteNombre() == null ? "Venta general" : s.getClienteNombre()),
                column("Método de pago", s -> s.getMetodoPago() == null ? "—" : s.getMetodoPago()),
                column("Unidades", s -> String.valueOf(s.getUnidadesVendidas())),
                column("Total", s -> currency.format(s.getTotal())),
                column("Estado", Venta::getStatus));
        VBox surface = new VBox(salesTable); surface.getStyleClass().add("surface-card");
        surface.setPadding(new Insets(12)); VBox.setVgrow(surface, Priority.ALWAYS);
        getChildren().addAll(heading, subtitle, surface);
        refresh();
    }

    private TableColumn<Venta, String> column(String title, java.util.function.Function<Venta, String> value) {
        TableColumn<Venta, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void refresh() {
        salesTable.setItems(FXCollections.observableArrayList(context.sales().listar()));
    }

    private void openSaleDialog() {
        List<Product> products = context.products().getAllProducts().stream()
                .filter(Product::isActive)
                .filter(p -> p.getCategory() != null && p.getCategory().isActive()).toList();
        List<Cliente> customers = context.clients().listar().stream().filter(Cliente::isActive).toList();
        List<MetodoPago> methods = context.getActivePaymentMethods();
        if (products.isEmpty()) { showError("No hay productos activos disponibles para vender."); return; }
        if (methods.isEmpty()) { showError("No hay métodos de pago activos configurados."); return; }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Registrar venta");
        dialog.setHeaderText("Agrega productos y confirma el método de pago.");
        ButtonType save = new ButtonType("Registrar venta", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        ComboBox<Cliente> customer = new ComboBox<>(FXCollections.observableArrayList(customers));
        customer.setPromptText("Cliente (opcional)"); customer.setMaxWidth(Double.MAX_VALUE);
        customer.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Cliente item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNombre());
            }
        });
        customer.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Cliente item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item.getNombre());
            }
        });

        ComboBox<MetodoPago> payment = new ComboBox<>(FXCollections.observableArrayList(methods));
        payment.setPromptText("Método de pago"); payment.setMaxWidth(Double.MAX_VALUE);
        payment.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(MetodoPago item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item.getName());
            }
        });
        payment.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(MetodoPago item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item.getName());
            }
        });

        ComboBox<Product> product = new ComboBox<>(FXCollections.observableArrayList(products));
        product.setPromptText("Selecciona un producto"); product.setMaxWidth(Double.MAX_VALUE);
        product.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " · "
                        + currency.format(item.getPrice()) + " · Stock: " + item.getStock());
            }
        });
        product.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item.getName());
            }
        });
        Spinner<Integer> quantity = new Spinner<>(1, 100000, 1); quantity.setEditable(true);
        Button add = new Button("Agregar");
        ObservableList<DetalleVenta> cart = FXCollections.observableArrayList();
        TableView<DetalleVenta> cartTable = new TableView<>(cart);
        cartTable.setPrefHeight(190);
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        cartTable.getColumns().addAll(
                detailColumn("Producto", DetalleVenta::getProductoNombre),
                detailColumn("Cantidad", d -> String.valueOf(d.getCantidad())),
                detailColumn("Precio", d -> currency.format(d.getPrecioUnitario())),
                detailColumn("Subtotal", d -> currency.format(d.getSubtotal())));
        Label total = new Label("Total: " + currency.format(0));
        total.getStyleClass().add("page-subtitle");
        add.setOnAction(event -> {
            Product selected = product.getValue();
            if (selected == null) { showError("Selecciona un producto."); return; }
            int amount = quantity.getValue();
            int current = cart.stream().filter(d -> d.getProductoId().equals(selected.getId()))
                    .mapToInt(DetalleVenta::getCantidad).sum();
            if (amount <= 0 || current + amount > selected.getStock()) {
                showError("La cantidad supera las existencias disponibles para " + selected.getName() + "."); return;
            }
            DetalleVenta existing = cart.stream()
                    .filter(d -> d.getProductoId().equals(selected.getId())).findFirst().orElse(null);
            if (existing == null) cart.add(new DetalleVenta(null, selected.getId(), selected.getName(),
                    selected.getCategory().getName(), amount, selected.getPrice()));
            else { existing.setCantidad(existing.getCantidad() + amount); cartTable.refresh(); }
            total.setText("Total: " + currency.format(cart.stream().mapToDouble(DetalleVenta::getSubtotal).sum()));
            product.getSelectionModel().clearSelection(); quantity.getValueFactory().setValue(1);
        });
        Button remove = new Button("Quitar seleccionado");
        remove.setOnAction(event -> {
            DetalleVenta selected = cartTable.getSelectionModel().getSelectedItem();
            if (selected != null) cart.remove(selected);
            total.setText("Total: " + currency.format(cart.stream().mapToDouble(DetalleVenta::getSubtotal).sum()));
        });

        GridPane form = new GridPane(); form.setHgap(12); form.setVgap(10); form.setPadding(new Insets(12));
        form.addRow(0, new Label("Cliente"), customer);
        form.addRow(1, new Label("Método de pago"), payment);
        HBox addControls = new HBox(10, product, quantity, add); HBox.setHgrow(product, Priority.ALWAYS);
        form.add(addControls, 0, 2, 2, 1);
        HBox cartFooter = new HBox(12, remove, new Region(), total);
        HBox.setHgrow(cartFooter.getChildren().get(1), Priority.ALWAYS);
        VBox cartBox = new VBox(8, new Label("Detalle de la venta"), cartTable, cartFooter);
        dialog.getDialogPane().setContent(new VBox(10, form, cartBox));
        dialog.getDialogPane().setPrefWidth(790); dialog.getDialogPane().setPrefHeight(570);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != save) return;
        if (cart.isEmpty()) { showError("Agrega al menos un producto a la venta."); return; }
        if (payment.getValue() == null) { showError("Selecciona un método de pago."); return; }
        try {
            Venta request = new Venta(null, LocalDateTime.now(),
                    customer.getValue() == null ? null : customer.getValue().getId(),
                    null, payment.getValue().getId(), "DRAFT", List.copyOf(cart));
            context.sales().registrar(request);
            refresh(); showInfo("La venta se registró correctamente.");
        } catch (RuntimeException error) {
            showError(error.getMessage() == null ? "No se pudo registrar la venta." : error.getMessage());
        }
    }

    private TableColumn<DetalleVenta, String> detailColumn(String title,
            java.util.function.Function<DetalleVenta, String> value) {
        TableColumn<DetalleVenta, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Venta no registrada"); alert.setHeaderText("Revisa la información");
        alert.setContentText(message); alert.showAndWait();
    }
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("AKRON"); alert.setHeaderText("Operación completada");
        alert.setContentText(message); alert.showAndWait();
    }
}
