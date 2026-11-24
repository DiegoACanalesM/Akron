package com.negocio.analitica.desktop;

import com.negocio.analitica.model.Product;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Initial inventory overview; stock-changing actions will be added in the next UI iteration. */
public final class InventoryView extends VBox {
    public InventoryView(DesktopContext context) {
        setSpacing(18); setPadding(new Insets(34));
        Label title = new Label("Inventario");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Productos que requieren revisión de existencias.");
        subtitle.getStyleClass().add("page-subtitle");
        TableView<Product> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(column("Producto", Product::getName),
                column("Existencias", p -> String.valueOf(p.getStock())),
                column("Stock mínimo", p -> String.valueOf(p.getMinimumStock())),
                column("Estado", p -> p.getStock() <= p.getMinimumStock() ? "Stock bajo" : "Disponible"));
        table.getItems().setAll(context.products().getLowStockProducts());
        VBox surface = new VBox(table);
        surface.getStyleClass().add("surface-card");
        surface.setPadding(new Insets(12));
        VBox.setVgrow(surface, Priority.ALWAYS);
        getChildren().addAll(title, subtitle, surface);
    }
    private TableColumn<Product, String> column(String title,
            java.util.function.Function<Product, String> value) {
        TableColumn<Product, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }
}
