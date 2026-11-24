package com.negocio.analitica.desktop;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Reports landing page; period filters and charts are planned for the next implementation stage. */
public final class ReportsView extends VBox {
    public ReportsView(DesktopContext context) {
        setSpacing(18); setPadding(new Insets(34));
        Label title = new Label("Reportes");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Análisis de ventas y rendimiento del inventario.");
        subtitle.getStyleClass().add("page-subtitle");
        Label message = new Label("La conexión con los servicios de analítica está preparada. Los filtros por período, gráficos y exportación se incorporarán en la siguiente etapa.");
        message.setWrapText(true);
        message.getStyleClass().add("muted-text");
        VBox surface = new VBox(message);
        surface.setPadding(new Insets(24));
        surface.getStyleClass().add("surface-card");
        getChildren().addAll(title, subtitle, surface);
    }
}
