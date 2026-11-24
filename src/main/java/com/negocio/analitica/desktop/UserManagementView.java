package com.negocio.analitica.desktop;

import com.negocio.analitica.model.Usuario;
import com.negocio.analitica.security.AuthenticatedSession;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Owner-only employee account and permission administration screen. */
public final class UserManagementView extends VBox {
    private final DesktopContext context;
    private final AuthenticatedSession session;
    private final TableView<Usuario> table = new TableView<>();
    private final TextField username = new TextField();
    private final TextField fullName = new TextField();
    private final PasswordField password = new PasswordField();
    private final VBox permissionList = new VBox(8);
    private final Map<String, CheckBox> permissionChecks = new LinkedHashMap<>();
    private final Label feedback = new Label();
    private Usuario selected;

    public UserManagementView(DesktopContext context, AuthenticatedSession session) {
        this.context = context;
        this.session = session;
        session.requirePermission("USERS_MANAGE");
        setSpacing(16);
        setPadding(new Insets(30));
        getStyleClass().add("page-content");

        Label title = new Label("Usuarios y permisos");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Crea cuentas de empleado y configura sus permisos efectivos.");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().addAll(
                column("Nombre", Usuario::getFullName),
                column("Usuario", Usuario::getUsername),
                column("Estado", u -> u.isActive() ? "Activo" : "Inactivo"));
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, value) -> selectUser(value));
        table.setPrefHeight(230);

        username.setPromptText("Nombre de usuario");
        fullName.setPromptText("Nombre completo");
        password.setPromptText("Contraseña (mínimo 10 caracteres)");
        for (TextField field : new TextField[]{username, fullName, password}) field.getStyleClass().add("input-field");
        Button create = new Button("Crear empleado");
        create.getStyleClass().add("primary-button");
        create.setOnAction(event -> createEmployee());
        HBox createForm = new HBox(10, username, fullName, password, create);
        createForm.setFillHeight(true);
        HBox.setHgrow(username, Priority.ALWAYS);
        HBox.setHgrow(fullName, Priority.ALWAYS);
        HBox.setHgrow(password, Priority.ALWAYS);

        Button toggle = new Button("Activar / desactivar seleccionado");
        toggle.setOnAction(event -> toggleSelected());
        Button restore = new Button("Restaurar permisos del rol");
        restore.setOnAction(event -> restoreDefaults());
        HBox userActions = new HBox(10, toggle, restore);

        Label permissionTitle = new Label("Permisos del empleado seleccionado");
        permissionTitle.getStyleClass().add("section-title");
        permissionList.getStyleClass().add("surface-card");
        permissionList.setPadding(new Insets(16));
        ScrollPane permissionScroll = new ScrollPane(permissionList);
        permissionScroll.setFitToWidth(true);
        permissionScroll.setPrefViewportHeight(230);

        feedback.getStyleClass().add("feedback");
        getChildren().addAll(title, subtitle, table, new Label("Crear cuenta de empleado"),
                createForm, userActions, permissionTitle, permissionScroll, feedback);
        refreshUsers();
        loadPermissionCatalog();
    }

    private TableColumn<Usuario, String> column(String title, java.util.function.Function<Usuario, String> value) {
        TableColumn<Usuario, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private void refreshUsers() {
        table.setItems(FXCollections.observableArrayList(context.listUsers(session)));
    }

    private void loadPermissionCatalog() {
        permissionList.getChildren().clear();
        permissionChecks.clear();
        Set<String> codes = context.allPermissionCodes();
        for (String code : codes.stream().sorted().toList()) {
            CheckBox check = new CheckBox(code);
            check.setDisable(true);
            check.setOnAction(event -> {
                if (selected == null || !context.isEmployee(selected.getId())) return;
                try {
                    context.setPermission(session, selected.getId(), code, check.isSelected());
                    feedback.setText("Permiso actualizado: " + code);
                } catch (RuntimeException error) {
                    feedback.setText(error.getMessage());
                    loadSelectedPermissions();
                }
            });
            permissionChecks.put(code, check);
            permissionList.getChildren().add(check);
        }
    }

    private void selectUser(Usuario user) {
        selected = user;
        loadSelectedPermissions();
    }

    private void loadSelectedPermissions() {
        if (selected == null) {
            permissionChecks.values().forEach(check -> { check.setSelected(false); check.setDisable(true); });
            return;
        }
        boolean employee = context.isEmployee(selected.getId());
        Set<String> effective = context.effectivePermissions(selected.getId());
        permissionChecks.forEach((code, check) -> {
            check.setDisable(!employee);
            check.setSelected(effective.contains(code));
        });
        feedback.setText(employee
                ? "Los cambios se aplican inmediatamente al usuario seleccionado."
                : "La cuenta seleccionada es propietaria; sus permisos no se modifican desde aquí.");
    }

    private void createEmployee() {
        try {
            context.createEmployee(session, username.getText(), fullName.getText(), password.getText().toCharArray());
            username.clear(); fullName.clear(); password.clear();
            refreshUsers();
            feedback.setText("Cuenta de empleado creada correctamente.");
        } catch (RuntimeException error) {
            feedback.setText(error.getMessage() == null ? "No se pudo crear el usuario." : error.getMessage());
        }
    }

    private void toggleSelected() {
        if (selected == null) {
            feedback.setText("Selecciona un usuario.");
            return;
        }
        if (selected.getId().equals(session.getUserId())) {
            feedback.setText("No puedes cambiar el estado de tu propia sesión.");
            return;
        }
        try {
            context.setUserActive(session, selected.getId(), !selected.isActive());
            selected = null;
            refreshUsers();
            loadSelectedPermissions();
            feedback.setText("Estado del usuario actualizado.");
        } catch (RuntimeException error) {
            feedback.setText(error.getMessage());
        }
    }

    private void restoreDefaults() {
        if (selected == null || !context.isEmployee(selected.getId())) {
            feedback.setText("Selecciona una cuenta de empleado.");
            return;
        }
        try {
            for (String code : permissionChecks.keySet()) {
                context.restorePermission(session, selected.getId(), code);
            }
            loadSelectedPermissions();
            feedback.setText("Se restauraron los permisos predeterminados del rol EMPLOYEE.");
        } catch (RuntimeException error) {
            feedback.setText(error.getMessage());
        }
    }
}
