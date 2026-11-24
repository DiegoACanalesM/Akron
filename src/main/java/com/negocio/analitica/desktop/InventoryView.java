package com.negocio.analitica.desktop;

import com.negocio.analitica.model.InventoryMovement;
import com.negocio.analitica.model.Product;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

/** Inventory workspace for stock entries, exits, adjustments and movement history. */
public final class InventoryView extends VBox {
    private final DesktopContext context;
    private final TableView<Product> table = new TableView<>();
    private final TextField search = new TextField();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public InventoryView(DesktopContext context) {
        this.context = context;
        setSpacing(18);
        setPadding(new Insets(34));
        Label title = new Label("Inventario");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Controla las existencias y consulta el historial de movimientos.");
        subtitle.getStyleClass().add("page-subtitle");
        search.setPromptText("Buscar producto...");
        search.setMaxWidth(440);
        search.getStyleClass().add("input-field");
        search.textProperty().addListener((obs, oldValue, newValue) -> refresh(newValue));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("data-table");
        table.getColumns().addAll(
                column("Producto", Product::getName),
                column("Categoría", p -> p.getCategory() == null ? "Sin categoría" : p.getCategory().getName()),
                column("Existencias", p -> Integer.toString(p.getStock())),
                column("Stock mínimo", p -> Integer.toString(p.getMinimumStock())),
                column("Estado", p -> p.getStock() <= p.getMinimumStock() ? "Stock bajo" : "Disponible"),
                actionColumn());
        VBox surface = new VBox(table);
        surface.getStyleClass().add("surface-card");
        surface.setPadding(new Insets(12));
        VBox.setVgrow(surface, Priority.ALWAYS);
        getChildren().addAll(title, subtitle, search, surface);
        refresh("");
    }

    private void refresh(String query) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        table.setItems(FXCollections.observableArrayList(context.products().getAllProducts().stream()
                .filter(Product::isActive)
                .filter(p -> p.getName().toLowerCase(Locale.ROOT).contains(term))
                .toList()));
    }

    private TableColumn<Product, String> column(String title, Function<Product, String> value) {
        TableColumn<Product, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private TableColumn<Product, Product> actionColumn() {
        TableColumn<Product, Product> column = new TableColumn<>("Operaciones");
        column.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue()));
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Button movement = new Button("Movimiento");
            private final Button history = new Button("Historial");
            private final HBox actions = new HBox(8, movement, history);
            {
                movement.setOnAction(event -> openMovementDialog(getItem()));
                history.setOnAction(event -> showHistory(getItem()));
            }
            @Override protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                setGraphic(empty || product == null ? null : actions);
            }
        });
        return column;
    }

    private void openMovementDialog(Product product) {
        if (product == null) return;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Movimiento de inventario");
        dialog.setHeaderText(product.getName() + " · Stock actual: " + product.getStock());
        ButtonType saveType = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(
                "Entrada", "Salida", "Ajustar stock"));
        type.getSelectionModel().selectFirst();
        TextField quantity = new TextField();
        quantity.setPromptText("Cantidad");
        TextField note = new TextField();
        note.setPromptText("Motivo o descripción (opcional)");
        Label quantityHelp = new Label("Unidades que ingresan al inventario.");
        type.valueProperty().addListener((obs, oldValue, newValue) -> {
            quantity.clear();
            quantityHelp.setText("Ajustar stock".equals(newValue)
                    ? "Escribe el nuevo stock total, no la diferencia."
                    : "Escribe la cantidad de unidades.");
            quantity.setPromptText("Ajustar stock".equals(newValue) ? "Nuevo stock total" : "Cantidad");
        });
        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10); form.setPadding(new Insets(12));
        form.addRow(0, new Label("Operación"), type);
        form.addRow(1, new Label("Cantidad"), quantity);
        form.addRow(2, new Label("Nota"), note);
        form.add(quantityHelp, 1, 3);
        GridPane.setHgrow(type, Priority.ALWAYS);
        GridPane.setHgrow(quantity, Priority.ALWAYS);
        GridPane.setHgrow(note, Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(470);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != saveType) return;

        try {
            int amount = Integer.parseInt(quantity.getText().trim());
            switch (type.getValue()) {
                case "Entrada" -> context.inventory().registerEntry(product.getId(), amount, note.getText());
                case "Salida" -> context.inventory().registerExit(product.getId(), amount, note.getText());
                case "Ajustar stock" -> context.inventory().adjustStock(product.getId(), amount, note.getText());
                default -> throw new IllegalArgumentException("Selecciona una operación válida.");
            }
            refresh(search.getText());
        } catch (NumberFormatException error) {
            showError("La cantidad debe ser un número entero.");
        } catch (RuntimeException error) {
            showError(error.getMessage() == null ? "No se pudo registrar el movimiento." : error.getMessage());
        }
    }

    private void showHistory(Product product) {
        if (product == null) return;
        try {
            var movements = context.inventory().getHistory(product.getId());
            TableView<InventoryMovement> history = new TableView<>();
            history.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            history.getColumns().addAll(
                    movementColumn("Fecha", m -> m.getCreatedAt() == null ? "—" : m.getCreatedAt().format(DATE_FORMAT)),
                    movementColumn("Tipo", m -> m.getMovementType().name()),
                    movementColumn("Cantidad", m -> Integer.toString(m.getQuantity())),
                    movementColumn("Antes", m -> Integer.toString(m.getStockBefore())),
                    movementColumn("Después", m -> Integer.toString(m.getStockAfter())),
                    movementColumn("Nota", m -> m.getNote() == null ? "" : m.getNote()));
            history.setItems(FXCollections.observableArrayList(movements));
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Historial de inventario");
            dialog.setHeaderText(product.getName());
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            VBox box = new VBox(10, history);
            box.setPrefSize(760, 390);
            dialog.getDialogPane().setContent(box);
            dialog.showAndWait();
        } catch (RuntimeException error) {
            showError(error.getMessage() == null ? "No se pudo consultar el historial." : error.getMessage());
        }
    }

    private TableColumn<InventoryMovement, String> movementColumn(String title,
            Function<InventoryMovement, String> value) {
        TableColumn<InventoryMovement, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Operación no completada");
        alert.setHeaderText("No se pudo completar la operación");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
