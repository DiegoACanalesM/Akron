package com.negocio.analitica.desktop;

import com.negocio.analitica.model.Product;
import com.negocio.analitica.security.AuthenticatedSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.text.NumberFormat;
import java.util.Locale;

/** Live dashboard indicators calculated from the existing application services. */
public final class DashboardView extends VBox {
    public DashboardView(DesktopContext context, AuthenticatedSession session) {
        setSpacing(22);
        setPadding(new Insets(34));
        getStyleClass().add("page-content");
        Label title = new Label("Dashboard");
        title.getStyleClass().add("page-title");
        Label greeting = new Label("Bienvenido, " + session.getFullName());
        greeting.getStyleClass().add("page-subtitle");

        var products = context.products().getAllProducts().stream().filter(Product::isActive).toList();
        var categories = context.categories().getAllCategories().stream().filter(c -> c.isActive()).toList();
        int lowStock = (int) products.stream().filter(p -> p.getStock() <= p.getMinimumStock()).count();
        double inventoryValue = products.stream().mapToDouble(p -> p.getPrice() * p.getStock()).sum();
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

        GridPane cards = new GridPane();
        cards.setHgap(16); cards.setVgap(16);
        cards.add(metric("Productos activos", String.valueOf(products.size()), false), 0, 0);
        cards.add(metric("Categorías", String.valueOf(categories.size()), false), 1, 0);
        cards.add(metric("Stock bajo", String.valueOf(lowStock), true), 2, 0);
        cards.add(metric("Valor del inventario", currency.format(inventoryValue), false), 3, 0);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS);
            cards.getColumnConstraints().add(column);
        }

        VBox summary = new VBox(12);
        summary.getStyleClass().add("surface-card");
        summary.setPadding(new Insets(24));
        Label summaryTitle = new Label("Resumen del inventario");
        summaryTitle.getStyleClass().add("section-title");
        Label summaryText = new Label("AKRON está conectado a PostgreSQL. Los indicadores se calculan con los productos y categorías registrados.");
        summaryText.getStyleClass().add("muted-text");
        summaryText.setWrapText(true);
        summary.getChildren().addAll(summaryTitle, summaryText);

        getChildren().addAll(title, greeting, cards, summary);
    }

    private VBox metric(String name, String value, boolean warning) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(22));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("metric-card");
        Label label = new Label(name);
        label.getStyleClass().add("muted-text");
        Label number = new Label(value);
        number.getStyleClass().add(warning ? "metric-warning" : "metric-value");
        card.getChildren().addAll(label, number);
        return card;
    }
}
