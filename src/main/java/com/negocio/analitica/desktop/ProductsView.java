package com.negocio.analitica.desktop;

import com.negocio.analitica.model.Category;
import com.negocio.analitica.model.Product;
import com.negocio.analitica.security.AuthenticatedSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** Product catalogue with create, edit, search and soft-deactivation actions. */
public final class ProductsView extends VBox {
    private final DesktopContext context;
    private final AuthenticatedSession session;
    private final TableView<Product> table = new TableView<>();
    private final TextField search = new TextField();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

    public ProductsView(DesktopContext context, AuthenticatedSession session) {
        this.context = context;
        this.session = session;
        setSpacing(18);
        setPadding(new Insets(34));

        Label title = new Label("Productos");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Administra el catálogo y consulta las existencias.");
        subtitle.getStyleClass().add("page-subtitle");

        Button add = new Button("Agregar producto");
        add.setDisable(!session.hasPermission("PRODUCTS_MANAGE"));
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> openProductDialog(null));

        HBox heading = new HBox(16, title, new Region(), add);
        HBox.setHgrow(heading.getChildren().get(1), Priority.ALWAYS);
        heading.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        search.setPromptText("Buscar por nombre o categoría...");
        search.getStyleClass().add("input-field");
        search.setMaxWidth(440);
        search.textProperty().addListener((obs, oldValue, newValue) -> refresh(newValue));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("data-table");
        table.getColumns().addAll(
                column("Producto", Product::getName),
                column("Categoría", p -> p.getCategory() == null ? "Sin categoría" : p.getCategory().getName()),
                column("Precio", p -> currency.format(p.getPrice())),
                column("Stock", p -> Integer.toString(p.getStock())),
                column("Stock mínimo", p -> Integer.toString(p.getMinimumStock())),
                column("Estado", p -> p.getStock() <= p.getMinimumStock() ? "Stock bajo" : "Disponible"),
                actionColumn());

        VBox surface = new VBox(table);
        surface.getStyleClass().add("surface-card");
        surface.setPadding(new Insets(12));
        VBox.setVgrow(surface, Priority.ALWAYS);
        getChildren().addAll(heading, subtitle, search, surface);
        refresh("");
    }

    private void refresh(String query) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Product> products = context.products().getAllProducts().stream()
                .filter(Product::isActive)
                .filter(p -> p.getName().toLowerCase(Locale.ROOT).contains(term)
                        || (p.getCategory() != null
                        && p.getCategory().getName().toLowerCase(Locale.ROOT).contains(term)))
                .toList();
        table.setItems(FXCollections.observableArrayList(products));
    }

    private TableColumn<Product, String> column(String title, Function<Product, String> value) {
        TableColumn<Product, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private TableColumn<Product, Product> actionColumn() {
        TableColumn<Product, Product> column = new TableColumn<>("Acciones");
        column.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue()));
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Button edit = new Button("Editar");
            private final Button deactivate = new Button("Desactivar");
            private final HBox actions = new HBox(8, edit, deactivate);
            {
                edit.setDisable(!session.hasPermission("PRODUCTS_MANAGE"));
                deactivate.setDisable(!session.hasPermission("PRODUCTS_MANAGE"));
                edit.setOnAction(event -> openProductDialog(getItem()));
                deactivate.setOnAction(event -> deactivate(getItem()));
            }
            @Override protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                setGraphic(empty || product == null ? null : actions);
            }
        });
        return column;
    }

    private void openProductDialog(Product selected) {
        session.requirePermission("PRODUCTS_MANAGE");
        boolean editing = selected != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Editar producto" : "Nuevo producto");
        dialog.setHeaderText(editing ? "Actualiza los datos del producto" : "Registra un producto en AKRON");
        ButtonType saveType = new ButtonType(editing ? "Guardar cambios" : "Crear producto",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField name = new TextField(editing ? selected.getName() : "");
        TextField price = new TextField(editing ? Double.toString(selected.getPrice()) : "");
        TextField minimum = new TextField(editing ? Integer.toString(selected.getMinimumStock()) : "0");
        TextField stock = new TextField(editing ? Integer.toString(selected.getStock()) : "0");
        TextArea description = new TextArea(editing && selected.getDescription() != null
                ? selected.getDescription() : "");
        description.setPrefRowCount(3);
        stock.setDisable(editing);
        stock.setPromptText("Existencias iniciales");
        price.setPromptText("Ej. 1500.00");
        minimum.setPromptText("Unidades");
        name.setPromptText("Nombre del producto");

        List<Category> activeCategories = context.categories().getAllCategories().stream()
                .filter(Category::isActive).toList();
        ComboBox<Category> category = new ComboBox<>(FXCollections.observableArrayList(activeCategories));
        category.setPromptText("Selecciona una categoría");
        category.setMaxWidth(Double.MAX_VALUE);
        category.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        category.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        if (editing && selected.getCategory() != null) {
            activeCategories.stream().filter(c -> c.getId().equals(selected.getCategory().getId()))
                    .findFirst().ifPresent(category::setValue);
        }

        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10); form.setPadding(new Insets(12));
        form.addRow(0, new Label("Nombre"), name);
        form.addRow(1, new Label("Categoría"), category);
        form.addRow(2, new Label("Precio"), price);
        form.addRow(3, new Label("Stock mínimo"), minimum);
        form.addRow(4, new Label(editing ? "Stock actual (solo lectura)" : "Stock inicial"), stock);
        form.addRow(5, new Label("Descripción"), description);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(145);
        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labelColumn, inputColumn);
        GridPane.setHgrow(name, Priority.ALWAYS);
        GridPane.setHgrow(category, Priority.ALWAYS);
        GridPane.setHgrow(price, Priority.ALWAYS);
        GridPane.setHgrow(minimum, Priority.ALWAYS);
        GridPane.setHgrow(stock, Priority.ALWAYS);
        GridPane.setHgrow(description, Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(540);

        dialog.setResultConverter(button -> button == saveType ? saveType : null);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveType) return;

        try {
            if (name.getText().isBlank()) throw new IllegalArgumentException("El nombre es obligatorio.");
            if (category.getValue() == null) throw new IllegalArgumentException("Selecciona una categoría activa.");
            double parsedPrice = Double.parseDouble(price.getText().trim().replace(",", "."));
            int parsedMinimum = Integer.parseInt(minimum.getText().trim());
            int parsedStock = Integer.parseInt(stock.getText().trim());
            Product product = new Product(editing ? selected.getId() : null,
                    name.getText().trim(), description.getText().trim(), parsedPrice,
                    parsedStock, parsedMinimum, category.getValue(), true);
            if (editing) context.products().updateProduct(product);
            else context.products().createProduct(product);
            refresh(search.getText());
        } catch (NumberFormatException error) {
            showError("Precio, stock mínimo y stock inicial deben ser valores numéricos válidos.");
        } catch (RuntimeException error) {
            showError(error.getMessage() == null ? "No se pudo guardar el producto." : error.getMessage());
        }
    }

    private void deactivate(Product product) {
        session.requirePermission("PRODUCTS_MANAGE");
        if (product == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Desactivar producto");
        confirmation.setHeaderText("¿Desactivar " + product.getName() + "?");
        confirmation.setContentText("El producto dejará de aparecer en el catálogo activo. Su historial se conservará.");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            product.setActive(false);
            context.products().updateProduct(product);
            refresh(search.getText());
        } catch (RuntimeException error) {
            showError(error.getMessage() == null ? "No se pudo desactivar el producto." : error.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Operación no completada");
        alert.setHeaderText("Revisa los datos");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
