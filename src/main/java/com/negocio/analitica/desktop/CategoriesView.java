package com.negocio.analitica.desktop;

import com.negocio.analitica.model.Category;
import com.negocio.analitica.security.AuthenticatedSession;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** Category catalogue with create, edit, search and soft-deactivation actions. */
public final class CategoriesView extends VBox {
    private final DesktopContext context;
    private final AuthenticatedSession session;
    private final TableView<Category> table = new TableView<>();
    private final TextField search = new TextField();

    public CategoriesView(DesktopContext context, AuthenticatedSession session) {
        this.context = context;
        this.session = session;
        setSpacing(18);
        setPadding(new Insets(34));

        Label title = new Label("Categorías");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Organiza los productos mediante categorías.");
        subtitle.getStyleClass().add("page-subtitle");
        Button add = new Button("Agregar categoría");
        add.setDisable(!session.hasPermission("CATEGORIES_MANAGE"));
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> openCategoryDialog(null));
        HBox heading = new HBox(16, title, new Region(), add);
        HBox.setHgrow(heading.getChildren().get(1), Priority.ALWAYS);
        heading.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        search.setPromptText("Buscar categoría...");
        search.getStyleClass().add("input-field");
        search.setMaxWidth(440);
        search.textProperty().addListener((obs, oldValue, newValue) -> refresh(newValue));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("data-table");
        table.getColumns().addAll(
                column("Nombre", Category::getName),
                column("Descripción", c -> c.getDescription() == null ? "" : c.getDescription()),
                column("Estado", c -> c.isActive() ? "Activa" : "Inactiva"),
                actionColumn());
        VBox surface = new VBox(table);
        surface.getStyleClass().add("surface-card");
        surface.setPadding(new Insets(12));
        VBox.setVgrow(surface, Priority.ALWAYS);
        getChildren().addAll(heading, subtitle, search, surface);
        refresh("");
    }

    private void refresh(String query) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        table.setItems(FXCollections.observableArrayList(context.categories().getAllCategories().stream()
                .filter(c -> c.getName().toLowerCase(Locale.ROOT).contains(term))
                .toList()));
    }

    private TableColumn<Category, String> column(String title, Function<Category, String> value) {
        TableColumn<Category, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return column;
    }

    private TableColumn<Category, Category> actionColumn() {
        TableColumn<Category, Category> column = new TableColumn<>("Acciones");
        column.setCellValueFactory(cell -> new javafx.beans.property.SimpleObjectProperty<>(cell.getValue()));
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Button edit = new Button("Editar");
            private final Button deactivate = new Button("Desactivar");
            private final HBox actions = new HBox(8, edit, deactivate);
            {
                edit.setDisable(!session.hasPermission("CATEGORIES_MANAGE"));
                deactivate.setDisable(!session.hasPermission("CATEGORIES_MANAGE"));
                edit.setOnAction(event -> openCategoryDialog(getItem()));
                deactivate.setOnAction(event -> deactivate(getItem()));
            }
            @Override protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                setGraphic(empty || category == null ? null : actions);
                deactivate.setDisable(category != null && !category.isActive());
            }
        });
        return column;
    }

    private void openCategoryDialog(Category selected) {
        session.requirePermission("CATEGORIES_MANAGE");
        boolean editing = selected != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Editar categoría" : "Nueva categoría");
        dialog.setHeaderText(editing ? "Actualiza la categoría" : "Registra una categoría");
        ButtonType saveType = new ButtonType(editing ? "Guardar cambios" : "Crear categoría",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField name = new TextField(editing ? selected.getName() : "");
        name.setPromptText("Nombre de la categoría");
        TextArea description = new TextArea(editing && selected.getDescription() != null
                ? selected.getDescription() : "");
        description.setPromptText("Descripción (opcional)");
        description.setPrefRowCount(3);
        GridPane form = new GridPane();
        form.setHgap(12); form.setVgap(10); form.setPadding(new Insets(12));
        form.addRow(0, new Label("Nombre"), name);
        form.addRow(1, new Label("Descripción"), description);
        GridPane.setHgrow(name, Priority.ALWAYS);
        GridPane.setHgrow(description, Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(460);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveType) return;

        try {
            if (name.getText().isBlank()) throw new IllegalArgumentException("El nombre es obligatorio.");
            Category category = new Category(editing ? selected.getId() : null,
                    name.getText().trim(), description.getText().trim(), editing && selected.isActive());
            if (editing) context.categories().updateCategory(category);
            else context.categories().createCategory(category);
            refresh(search.getText());
        } catch (RuntimeException error) {
            showError(error.getMessage() == null ? "No se pudo guardar la categoría." : error.getMessage());
        }
    }

    private void deactivate(Category category) {
        session.requirePermission("CATEGORIES_MANAGE");
        if (category == null || !category.isActive()) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Desactivar categoría");
        confirmation.setHeaderText("¿Desactivar " + category.getName() + "?");
        confirmation.setContentText("Los productos existentes conservarán esta categoría, pero no podrá asignarse a nuevos productos.");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            category.setActive(false);
            context.categories().updateCategory(category);
            refresh(search.getText());
        } catch (RuntimeException error) {
            showError(error.getMessage() == null ? "No se pudo desactivar la categoría." : error.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Operación no completada");
        alert.setHeaderText("Revisa los datos");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
