package com.negocio.analitica.desktop;

import com.negocio.analitica.model.Product;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.text.NumberFormat;
import java.util.Locale;

/** Read-only first iteration of the product catalogue screen. */
public final class ProductsView extends VBox {
    public ProductsView(DesktopContext context) {
        setSpacing(18); setPadding(new Insets(34));
        Label title = new Label("Productos");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Consulta los productos registrados en AKRON.");
        subtitle.getStyleClass().add("page-subtitle");

        TextField search = new TextField();
        search.setPromptText("Buscar producto...");
        search.getStyleClass().add("input-field");
        search.setMaxWidth(420);

        TableView<Product> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("data-table");
        TableColumn<Product, String> name = column("Producto", p -> p.getName());
        TableColumn<Product, String> category = column("Categoría",
                p -> p.getCategory() == null ? "Sin categoría" : p.getCategory().getName());
        TableColumn<Product, String> price = column("Precio",
                p -> NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO")).format(p.getPrice()));
        TableColumn<Product, String> stock = column("Stock", p -> String.valueOf(p.getStock()));
        TableColumn<Product, String> minimum = column("Stock mínimo", p -> String.valueOf(p.getMinimumStock()));
        TableColumn<Product, String> status = column("Estado",
                p -> p.getStock() <= p.getMinimumStock() ? "Stock bajo" : "Disponible");
        table.getColumns().addAll(name, category, price, stock, minimum, status);

        var allProducts = context.products().getAllProducts().stream()
                .filter(Product::isActive).toList();
        table.getItems().setAll(allProducts);
        search.textProperty().addListener((obs, oldValue, newValue) -> table.getItems().setAll(
                allProducts.stream().filter(p -> p.getName().toLowerCase(Locale.ROOT)
                        .contains(newValue.toLowerCase(Locale.ROOT))).toList()));

        VBox surface = new VBox(table);
        surface.getStyleClass().add("surface-card");
        surface.setPadding(new Insets(12));
        VBox.setVgrow(surface, Priority.ALWAYS);
        getChildren().addAll(title, subtitle, search, surface);
    }

    private TableColumn<Product, String> column(String title,
            java.util.function.Function<Product, String> value) {
        TableColumn<Product, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }
}
