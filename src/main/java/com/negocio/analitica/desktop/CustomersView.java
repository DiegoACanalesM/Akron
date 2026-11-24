package com.negocio.analitica.desktop;

import com.negocio.analitica.model.Cliente;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.property.SimpleStringProperty;

/** Customer directory with create, edit, search and soft-deactivation actions. */
public final class CustomersView extends VBox {
    private final DesktopContext context;
    private final TableView<Cliente> table = new TableView<>();
    private final TextField name = new TextField();
    private final TextField document = new TextField();
    private final TextField phone = new TextField();
    private final TextField address = new TextField();
    private final TextField search = new TextField();
    private final Label feedback = new Label();
    private FilteredList<Cliente> filtered;

    public CustomersView(DesktopContext context) {
        this.context = context;
        setSpacing(16);
        setPadding(new Insets(30));
        getStyleClass().add("page-content");

        Label title = new Label("Clientes");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Administra la información de contacto de tus clientes.");
        subtitle.getStyleClass().add("page-subtitle");

        search.setPromptText("Buscar por nombre, documento o teléfono");
        search.textProperty().addListener((obs, oldValue, value) -> applyFilter(value));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                column("Nombre", c -> c.getNombre()),
                column("Documento", c -> c.getDocumento()),
                column("Teléfono", c -> c.getTelefono()),
                column("Dirección", c -> c.getDireccion()));
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> load(selected));
        table.setPrefHeight(360);

        name.setPromptText("Nombre completo");
        document.setPromptText("Documento (opcional)");
        phone.setPromptText("Teléfono");
        address.setPromptText("Dirección");
        for (TextField field : new TextField[]{name, document, phone, address}) {
            field.getStyleClass().add("input-field");
        }

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.add(new Label("Nombre"), 0, 0); form.add(name, 1, 0);
        form.add(new Label("Documento"), 2, 0); form.add(document, 3, 0);
        form.add(new Label("Teléfono"), 0, 1); form.add(phone, 1, 1);
        form.add(new Label("Dirección"), 2, 1); form.add(address, 3, 1);
        for (int i = 1; i < 4; i++) GridPane.setHgrow(form.getChildren().get(i), Priority.ALWAYS);

        Button save = new Button("Guardar cliente");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> save());
        Button clear = new Button("Limpiar");
        clear.setOnAction(event -> clearForm());
        Button deactivate = new Button("Desactivar");
        deactivate.setOnAction(event -> deactivate());
        HBox actions = new HBox(10, save, clear, deactivate);
        feedback.getStyleClass().add("feedback");

        VBox panel = new VBox(12, new Label("Datos del cliente"), form, actions, feedback);
        panel.getStyleClass().add("surface-card");
        panel.setPadding(new Insets(18));
        getChildren().addAll(title, subtitle, search, table, panel);
        refresh();
    }

    private TableColumn<Cliente, String> column(String title, java.util.function.Function<Cliente, String> value) {
        TableColumn<Cliente, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(
                value.apply(cell.getValue()) == null ? "—" : value.apply(cell.getValue())));
        return column;
    }

    private void refresh() {
        filtered = new FilteredList<>(FXCollections.observableArrayList(context.clients().listar()), c -> true);
        table.setItems(filtered);
        applyFilter(search.getText());
    }

    private void applyFilter(String query) {
        if (filtered == null) return;
        String q = query == null ? "" : query.trim().toLowerCase();
        filtered.setPredicate(c -> q.isEmpty()
                || contains(c.getNombre(), q) || contains(c.getDocumento(), q)
                || contains(c.getTelefono(), q) || contains(c.getDireccion(), q));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void load(Cliente customer) {
        if (customer == null) return;
        name.setText(customer.getNombre());
        document.setText(customer.getDocumento() == null ? "" : customer.getDocumento());
        phone.setText(customer.getTelefono() == null ? "" : customer.getTelefono());
        address.setText(customer.getDireccion() == null ? "" : customer.getDireccion());
    }

    private void save() {
        try {
            if (name.getText().isBlank()) {
                feedback.setText("El nombre del cliente es obligatorio.");
                return;
            }
            Cliente selected = table.getSelectionModel().getSelectedItem();
            Cliente customer = new Cliente(selected == null ? null : selected.getId(),
                    name.getText().trim(), blankToNull(document.getText()),
                    blankToNull(phone.getText()), blankToNull(address.getText()),
                    selected == null || selected.isActive());
            if (selected == null) context.clients().crear(customer);
            else context.clients().actualizar(customer);
            feedback.setText("Cliente guardado correctamente.");
            clearForm();
            refresh();
        } catch (RuntimeException error) {
            feedback.setText(error.getMessage() == null ? "No se pudo guardar el cliente." : error.getMessage());
        }
    }

    private void deactivate() {
        Cliente selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            feedback.setText("Selecciona un cliente de la tabla.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Deseas desactivar a " + selected.getNombre() + "?", ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Desactivar cliente");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            context.clients().eliminar(selected.getId());
            clearForm();
            refresh();
            feedback.setText("Cliente desactivado.");
        } catch (RuntimeException error) {
            feedback.setText(error.getMessage());
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void clearForm() {
        table.getSelectionModel().clearSelection();
        name.clear(); document.clear(); phone.clear(); address.clear();
    }
}
