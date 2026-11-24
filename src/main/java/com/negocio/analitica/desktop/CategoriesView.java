package com.negocio.analitica.desktop;

import com.negocio.analitica.model.Category;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Category catalogue screen connected to the category controller. */
public final class CategoriesView extends VBox {
    public CategoriesView(DesktopContext context) {
        setSpacing(18); setPadding(new Insets(34));
        Label title = new Label("Categorías");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Consulta las categorías registradas.");
        subtitle.getStyleClass().add("page-subtitle");
        TableView<Category> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(column("Nombre", Category::getName),
                column("Descripción", c -> c.getDescription() == null ? "" : c.getDescription()),
                column("Estado", c -> c.isActive() ? "Activa" : "Inactiva"));
        table.getItems().setAll(context.categories().getAllCategories());
        VBox surface = new VBox(table);
        surface.getStyleClass().add("surface-card");
        surface.setPadding(new Insets(12));
        VBox.setVgrow(surface, Priority.ALWAYS);
        getChildren().addAll(title, subtitle, surface);
    }
    private TableColumn<Category, String> column(String title,
            java.util.function.Function<Category, String> value) {
        TableColumn<Category, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }
}
