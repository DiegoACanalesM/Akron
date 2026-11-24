package com.negocio.analitica.desktop;

import com.negocio.analitica.model.Product;
import com.negocio.analitica.security.AuthenticatedSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

/** Commercial and inventory indicators calculated from the current application data. */
public final class DashboardView extends VBox {
    public DashboardView(DesktopContext context, AuthenticatedSession session) {
        setSpacing(20);
        setPadding(new Insets(30));
        getStyleClass().add("page-content");

        Label title = new Label("Dashboard");
        title.getStyleClass().add("page-title");
        Label greeting = new Label("Bienvenido, " + session.getFullName());
        greeting.getStyleClass().add("page-subtitle");

        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        boolean canViewProducts = session.hasPermission("PRODUCTS_VIEW");
        boolean canViewCategories = session.hasPermission("CATEGORIES_VIEW");
        var products = canViewProducts
                ? context.products().getAllProducts().stream().filter(Product::isActive).toList()
                : java.util.List.<Product>of();
        var categories = canViewCategories
                ? context.categories().getAllCategories().stream().filter(c -> c.isActive()).toList()
                : java.util.List.of();
        int lowStock = canViewProducts
                ? (int) products.stream().filter(p -> p.getStock() <= p.getMinimumStock()).count() : 0;
        double inventoryValue = canViewProducts
                ? products.stream().mapToDouble(p -> p.getPrice() * p.getStock()).sum() : 0;

        GridPane cards = new GridPane();
        cards.setHgap(14); cards.setVgap(14);
        cards.add(metric("Productos activos", canViewProducts ? String.valueOf(products.size()) : "Sin permiso", false), 0, 0);
        cards.add(metric("Categorías activas", canViewCategories ? String.valueOf(categories.size()) : "Sin permiso", false), 1, 0);
        cards.add(metric("Productos con stock bajo", canViewProducts ? String.valueOf(lowStock) : "Sin permiso", lowStock > 0), 2, 0);
        cards.add(metric("Valor del inventario", canViewProducts ? currency.format(inventoryValue) : "Sin permiso", false), 0, 1, 3, 1);

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        if (session.hasPermission("ANALYTICS_VIEW")) {
            var analytics = context.analytics();
            double revenue = analytics.totalVentas(monthStart, today);
            int transactions = analytics.cantidadTransacciones(monthStart, today);
            int units = analytics.unidadesVendidas(monthStart, today);
            double average = analytics.ticketPromedio(monthStart, today);
            cards.add(metric("Ingresos del mes", currency.format(revenue), false), 0, 2);
            cards.add(metric("Ventas del mes", String.valueOf(transactions), false), 1, 2);
            cards.add(metric("Unidades vendidas", String.valueOf(units), false), 2, 2);
            cards.add(metric("Ticket promedio", currency.format(average), false), 0, 3, 3, 1);
        } else {
            VBox restricted = new VBox(8,
                    new Label("Indicadores comerciales"),
                    new Label("Tu cuenta no tiene permiso para consultar los indicadores de ventas."));
            restricted.getStyleClass().add("surface-card");
            restricted.setPadding(new Insets(18));
            cards.add(restricted, 0, 2, 3, 1);
        }

        for (int i = 0; i < 3; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(33.333);
            column.setHgrow(Priority.ALWAYS);
            cards.getColumnConstraints().add(column);
        }

        VBox summary = new VBox(10);
        summary.getStyleClass().add("surface-card");
        summary.setPadding(new Insets(22));
        Label summaryTitle = new Label("Resumen comercial");
        summaryTitle.getStyleClass().add("section-title");
        Label summaryText = new Label("Los indicadores comerciales corresponden al mes actual y se calculan con las ventas registradas. El inventario refleja productos activos y sus existencias actuales.");
        summaryText.getStyleClass().add("muted-text");
        summaryText.setWrapText(true);
        summary.getChildren().addAll(summaryTitle, summaryText);
        getChildren().addAll(title, greeting, cards, summary);
    }

    private VBox metric(String name, String value, boolean warning) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("metric-card");
        Label label = new Label(name);
        label.getStyleClass().add("muted-text");
        Label number = new Label(value);
        number.getStyleClass().add(warning ? "metric-warning" : "metric-value");
        number.setWrapText(true);
        card.getChildren().addAll(label, number);
        return card;
    }
}
