package com.negocio.analitica.desktop;

import com.negocio.analitica.security.AuthenticatedSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.function.Consumer;

/** Login and secure first-owner setup screen. */
public final class LoginView extends BorderPane {
    private final DesktopContext context;
    private final Consumer<AuthenticatedSession> onAuthenticated;
    private TextField username;
    private TextField fullName;
    private PasswordField password;
    private PasswordField confirmation;
    private Label feedback = new Label();

    public LoginView(DesktopContext context, Consumer<AuthenticatedSession> onAuthenticated) {
        this.context = context;
        this.onAuthenticated = onAuthenticated;
        getStyleClass().add("login-root");
        setLeft(buildBrand());
        setCenter(buildForm());
        getStylesheets().add(getClass().getResource("/styles/akron.css").toExternalForm());
    }

    private VBox buildBrand() {
        VBox brand = new VBox(22);
        brand.setAlignment(Pos.CENTER);
        brand.setPadding(new Insets(48));
        brand.setPrefWidth(420);
        brand.getStyleClass().add("brand-panel");
        Label logo = new Label("AKRON");
        logo.getStyleClass().add("brand-logo");
        Label subtitle = new Label("Sales & Inventory Management");
        subtitle.getStyleClass().add("brand-subtitle");
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label slogan = new Label("Mejores decisiones,\npara negocios más fuertes.");
        slogan.getStyleClass().add("brand-slogan");
        brand.getChildren().addAll(logo, subtitle, spacer, slogan);
        return brand;
    }

    private VBox buildForm() {
        username = new TextField();
        fullName = new TextField();
        password = new PasswordField();
        confirmation = new PasswordField();
        feedback = new Label();
        VBox form = new VBox(14);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setMaxWidth(430);
        form.setPadding(new Insets(42));
        Label title = new Label(context.needsOwnerSetup() ? "Configura tu cuenta" : "Bienvenido de nuevo");
        title.getStyleClass().add("page-title");
        Label description = new Label(context.needsOwnerSetup()
                ? "Crea la cuenta propietaria para comenzar a usar AKRON."
                : "Ingresa tus credenciales para continuar.");
        description.getStyleClass().add("muted-text");
        username.setPromptText("Nombre de usuario");
        password.setPromptText("Contraseña");
        styleField(username); styleField(password);
        form.getChildren().addAll(title, description, new Label("Usuario"), username);
        if (context.needsOwnerSetup()) {
            fullName.setPromptText("Nombre completo");
            confirmation.setPromptText("Confirmar contraseña");
            styleField(fullName); styleField(confirmation);
            form.getChildren().addAll(new Label("Nombre completo"), fullName,
                    new Label("Contraseña (mínimo 10 caracteres)"), password,
                    new Label("Confirmación"), confirmation);
        } else {
            form.getChildren().addAll(new Label("Contraseña"), password);
        }
        feedback.getStyleClass().add("feedback");
        Button submit = new Button(context.needsOwnerSetup() ? "Crear cuenta" : "Iniciar sesión");
        submit.getStyleClass().add("primary-button");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(event -> submit());
        password.setOnAction(event -> submit());
        form.getChildren().addAll(feedback, submit);
        return form;
    }

    private void styleField(TextField field) {
        field.getStyleClass().add("input-field");
        field.setMaxWidth(Double.MAX_VALUE);
    }

    private void submit() {
        try {
            String user = username.getText().trim();
            if (user.isBlank() || password.getText().isEmpty()) {
                feedback.setText("Completa el usuario y la contraseña.");
                return;
            }
            if (context.needsOwnerSetup()) {
                if (fullName.getText().isBlank() || password.getText().length() < 10) {
                    feedback.setText("Completa el nombre y usa al menos 10 caracteres.");
                    return;
                }
                if (!password.getText().equals(confirmation.getText())) {
                    feedback.setText("Las contraseñas no coinciden.");
                    return;
                }
                context.createOwner(user, fullName.getText(), password.getText().toCharArray());
                password.clear();
                confirmation.clear();
                setCenter(buildForm());
                return;
            }
            AuthenticatedSession session = context.login(user, password.getText().toCharArray());
            if (session == null) {
                feedback.setText("Credenciales incorrectas o cuenta inactiva.");
                password.clear();
                return;
            }
            context.activateSession(session);
            onAuthenticated.accept(session);
        } catch (RuntimeException error) {
            feedback.setText(error.getMessage() == null ? "No se pudo completar la operación." : error.getMessage());
        }
    }

}
