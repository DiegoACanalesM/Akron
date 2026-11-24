package com.negocio.analitica.desktop;

import com.negocio.analitica.model.Product;
import com.negocio.analitica.model.Venta;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Interactive reports connected to the PostgreSQL-backed analytics service. */
public final class ReportsView extends VBox {
    private final DesktopContext context;
    private final DatePicker from = new DatePicker(LocalDate.now().withDayOfMonth(1));
    private final DatePicker to = new DatePicker(LocalDate.now());
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    private final Label revenue = metricValue();
    private final Label transactions = metricValue();
    private final Label units = metricValue();
    private final Label average = metricValue();
    private final Label variation = metricValue();
    private final Label bestProduct = new Label("—");
    private final BarChart<String, Number> monthlyChart;
    private final BarChart<String, Number> categoryChart;
    private final BarChart<String, Number> paymentChart;
    private final TableView<Product> lowStockTable = new TableView<>();

    public ReportsView(DesktopContext context) {
        this.context = context;
        setSpacing(18); setPadding(new Insets(34));
        Label title = new Label("Reportes"); title.getStyleClass().add("page-title");
        Label subtitle = new Label("Analiza las ventas y el estado del inventario.");
        subtitle.getStyleClass().add("page-subtitle");
        from.setMaxWidth(170); to.setMaxWidth(170);
        Button apply = new Button("Aplicar período"); apply.getStyleClass().add("primary-button");
        apply.setOnAction(event -> refresh());
        HBox filters = new HBox(12, new Label("Desde"), from, new Label("Hasta"), to, apply);
        filters.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        GridPane metrics = new GridPane(); metrics.setHgap(12); metrics.setVgap(12);
        metrics.add(metricCard("Ingresos", revenue), 0, 0);
        metrics.add(metricCard("Transacciones", transactions), 1, 0);
        metrics.add(metricCard("Unidades vendidas", units), 2, 0);
        metrics.add(metricCard("Ticket promedio", average), 3, 0);
        metrics.add(metricCard("Variación vs. período anterior", variation), 0, 1, 2, 1);
        metrics.add(metricCard("Producto más vendido", bestProduct), 2, 1, 2, 1);
        for (int i = 0; i < 4; i++) GridPane.setHgrow(metrics.getChildren().get(i), Priority.ALWAYS);

        CategoryAxis monthAxis = new CategoryAxis();
        NumberAxis revenueAxis = new NumberAxis();
        monthlyChart = new BarChart<>(monthAxis, revenueAxis);
        monthlyChart.setTitle("Ingresos por mes"); monthlyChart.setLegendVisible(false);
        monthlyChart.setAnimated(false); monthlyChart.setPrefHeight(300);

        CategoryAxis categoryAxis = new CategoryAxis();
        NumberAxis categoryValues = new NumberAxis();
        categoryChart = new BarChart<>(categoryAxis, categoryValues);
        categoryChart.setTitle("Ingresos por categoría"); categoryChart.setLegendVisible(false);
        categoryChart.setAnimated(false); categoryChart.setPrefHeight(270);

        CategoryAxis paymentAxis = new CategoryAxis();
        NumberAxis paymentValues = new NumberAxis();
        paymentChart = new BarChart<>(paymentAxis, paymentValues);
        paymentChart.setTitle("Ingresos por método de pago"); paymentChart.setLegendVisible(false);
        paymentChart.setAnimated(false); paymentChart.setPrefHeight(270);

        lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        lowStockTable.getColumns().addAll(
                productColumn("Producto", Product::getName),
                productColumn("Stock actual", p -> String.valueOf(p.getStock())),
                productColumn("Stock mínimo", p -> String.valueOf(p.getMinimumStock())));
        lowStockTable.setPrefHeight(220);
        VBox lowStockCard = new VBox(10, new Label("Productos con stock bajo"), lowStockTable);
        lowStockCard.getStyleClass().add("surface-card"); lowStockCard.setPadding(new Insets(18));

        HBox charts = new HBox(14, chartCard(categoryChart), chartCard(paymentChart));
        HBox.setHgrow(charts.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(charts.getChildren().get(1), Priority.ALWAYS);
        VBox monthlyCard = chartCard(monthlyChart);
        getChildren().addAll(title, subtitle, filters, metrics, monthlyCard, charts, lowStockCard);
        refresh();
    }

    private Label metricValue() {
        Label label = new Label("—"); label.getStyleClass().add("metric-value"); return label;
    }

    private VBox metricCard(String title, Label value) {
        Label heading = new Label(title); heading.getStyleClass().add("muted-text");
        VBox card = new VBox(8, heading, value);
        card.getStyleClass().add("surface-card"); card.setPadding(new Insets(18));
        card.setMinWidth(150); return card;
    }

    private VBox chartCard(BarChart<String, Number> chart) {
        VBox card = new VBox(chart); card.getStyleClass().add("surface-card");
        card.setPadding(new Insets(12)); VBox.setVgrow(chart, Priority.ALWAYS); return card;
    }

    private TableColumn<Product, String> productColumn(String title,
            java.util.function.Function<Product, String> value) {
        TableColumn<Product, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void refresh() {
        LocalDate start = from.getValue();
        LocalDate end = to.getValue();
        if (start == null || end == null || end.isBefore(start)) {
            showError("Selecciona un período válido. La fecha final no puede ser anterior a la inicial.");
            return;
        }
        try {
            var analytics = context.analytics();
            double total = analytics.totalVentas(start, end);
            int count = analytics.cantidadTransacciones(start, end);
            revenue.setText(currency.format(total));
            transactions.setText(String.valueOf(count));
            units.setText(String.valueOf(analytics.unidadesVendidas(start, end)));
            average.setText(currency.format(analytics.ticketPromedio(start, end)));
            bestProduct.setText(analytics.productoMasVendido(start, end).orElse("Sin ventas"));

            long days = ChronoUnit.DAYS.between(start, end) + 1;
            LocalDate previousEnd = start.minusDays(1);
            LocalDate previousStart = previousEnd.minusDays(days - 1);
            double previousTotal = analytics.totalVentas(previousStart, previousEnd);
            double change = analytics.variacionPorcentual(previousTotal, total);
            variation.setText(String.format(Locale.forLanguageTag("es-CO"), "%+.1f%%", change));

            Map<YearMonth, Double> monthly = new LinkedHashMap<>();
            for (Venta sale : analytics.ventasEntre(start, end)) {
                if (sale.getFecha() != null) monthly.merge(YearMonth.from(sale.getFecha()), sale.getTotal(), Double::sum);
            }
            XYChart.Series<String, Number> monthlySeries = new XYChart.Series<>();
            DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-CO"));
            monthly.forEach((month, amount) -> monthlySeries.getData().add(
                    new XYChart.Data<>(month.atDay(1).format(monthFormat), amount)));
            monthlyChart.getData().setAll(monthlySeries);

            setChartData(categoryChart, analytics.ventasPorCategoria(start, end));
            setChartData(paymentChart, analytics.ventasPorMetodoPago(start, end));
            lowStockTable.setItems(FXCollections.observableArrayList(analytics.productosConStockBajo()));
        } catch (RuntimeException error) {
            showError(error.getMessage() == null ? "No se pudieron generar los reportes." : error.getMessage());
        }
    }

    private void setChartData(BarChart<String, Number> chart, Map<String, Double> values) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        values.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));
        chart.getData().setAll(series);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Reportes"); alert.setHeaderText("No se pudo actualizar la información");
        alert.setContentText(message); alert.showAndWait();
    }
}
