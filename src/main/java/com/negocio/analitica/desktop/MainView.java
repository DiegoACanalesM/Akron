package com.negocio.analitica.desktop;

import com.negocio.analitica.security.AuthenticatedSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Main desktop shell with a persistent navigation rail. */
public final class MainView extends BorderPane {
    private final DesktopContext context;
    private final AuthenticatedSession session;
    private final VBox navigation = new VBox(8);
    private final StackPane content = new StackPane();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();

    public MainView(DesktopContext context, AuthenticatedSession session, Runnable logout) {
        this.context = context;
        this.session = session;
        getStyleClass().add("app-root");
        setLeft(buildSidebar(logout));
        setCenter(content);
        getStylesheets().add(getClass().getResource("/styles/akron.css").toExternalForm());
        showPage("Dashboard");
    }

    private VBox buildSidebar(Runnable logout) {
        VBox sidebar = new VBox(22);
        sidebar.setPadding(new Insets(28, 16, 22, 16));
        sidebar.setPrefWidth(238);
        sidebar.getStyleClass().add("sidebar");
        Label logo = new Label("AKRON");
        logo.getStyleClass().add("sidebar-logo");
        Label subtitle = new Label("Sales & Inventory");
        subtitle.getStyleClass().add("sidebar-subtitle");
        navigation.getChildren().add(navButton("Dashboard"));
        if (session.hasPermission("PRODUCTS_VIEW")) navigation.getChildren().add(navButton("Products"));
        if (session.hasPermission("CATEGORIES_VIEW")) navigation.getChildren().add(navButton("Categories"));
        if (session.hasPermission("SALES_VIEW")) navigation.getChildren().add(navButton("Sales"));
        if (session.hasPermission("INVENTORY_VIEW")) navigation.getChildren().add(navButton("Inventory"));
        if (session.hasPermission("ANALYTICS_VIEW")) navigation.getChildren().add(navButton("Reports"));
        if (session.hasPermission("CUSTOMERS_VIEW")) navigation.getChildren().add(navButton("Customers"));
        if (session.hasPermission("USERS_MANAGE")) navigation.getChildren().add(navButton("Users"));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Button logoutButton = new Button("Cerrar sesión");
        logoutButton.getStyleClass().add("nav-button");
        logoutButton.setMaxWidth(Double.MAX_VALUE);
        logoutButton.setOnAction(event -> logout.run());
        sidebar.getChildren().addAll(logo, subtitle, navigation, spacer, logoutButton);
        return sidebar;
    }

    private Button navButton(String name) {
        Button button = new Button(name);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> showPage(name));
        navButtons.put(name, button);
        return button;
    }

    private void showPage(String name) {
        navButtons.forEach((key, button) -> button.getStyleClass().remove("nav-button-active"));
        navButtons.get(name).getStyleClass().add("nav-button-active");
        content.getChildren().clear();
        try {
            switch (name) {
                case "Dashboard" -> content.getChildren().add(new DashboardView(context, session));
                case "Products" -> {
                    session.requirePermission("PRODUCTS_VIEW");
                    content.getChildren().add(new ProductsView(context, session));
                }
                case "Categories" -> {
                    session.requirePermission("CATEGORIES_VIEW");
                    content.getChildren().add(new CategoriesView(context, session));
                }
                case "Sales" -> {
                    session.requirePermission("SALES_VIEW");
                    content.getChildren().add(new SalesView(context));
                }
                case "Inventory" -> {
                    session.requirePermission("INVENTORY_VIEW");
                    content.getChildren().add(new InventoryView(context));
                }
                case "Reports" -> {
                    session.requirePermission("ANALYTICS_VIEW");
                    content.getChildren().add(new ReportsView(context));
                }
                case "Customers" -> {
                    session.requirePermission("CUSTOMERS_VIEW");
                    content.getChildren().add(new CustomersView(context, session));
                }
                case "Users" -> {
                    session.requirePermission("USERS_MANAGE");
                    content.getChildren().add(new UserManagementView(context, session));
                }
                default -> throw new IllegalArgumentException("Unknown page");
            }
        } catch (RuntimeException error) {
            Label message = new Label("No se pudo cargar " + name + ": " + error.getMessage());
            message.getStyleClass().add("error-text");
            content.getChildren().add(message);
        }
    }
}
