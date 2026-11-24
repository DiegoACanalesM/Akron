package com.negocio.analitica.desktop;

import com.negocio.analitica.security.AuthenticatedSession;
import com.negocio.analitica.SingleInstanceGuard;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;

/** JavaFX desktop entry point. Business rules remain in the existing service layer. */
public final class AkronDesktopApplication extends Application {
    private SingleInstanceGuard instanceGuard;

    @Override
    public void start(Stage stage) {
        try {
            instanceGuard = SingleInstanceGuard.acquire(Path.of("data", "akron.lock"));
            DesktopContext context = new DesktopContext();
            LoginView loginView = new LoginView(context, session -> showMain(stage, context, session));
            stage.setTitle("AKRON | Gestión comercial");
            stage.setMinWidth(900);
            stage.setMinHeight(620);
            stage.setScene(new Scene(loginView, 1100, 720));
            stage.show();
        } catch (Exception error) {
            showStartupError(stage, error);
        }
    }

    private void showMain(Stage stage, DesktopContext context, AuthenticatedSession session) {
        MainView main = new MainView(context, session, () -> {
            LoginView login = new LoginView(context, next -> showMain(stage, context, next));
            stage.setScene(new Scene(login, 1100, 720));
        });
        stage.setScene(new Scene(main, 1280, 800));
        stage.centerOnScreen();
    }

    private void showStartupError(Stage stage, Exception error) {
        javafx.scene.control.Label message = new javafx.scene.control.Label(
                "No se pudo iniciar AKRON. Verifica PostgreSQL y las variables AKRON_DB_URL, AKRON_DB_USER y AKRON_DB_PASSWORD.\n\n"
                        + error.getMessage());
        message.setWrapText(true);
        message.setPadding(new javafx.geometry.Insets(24));
        stage.setTitle("AKRON | Error de inicio");
        stage.setScene(new Scene(message, 700, 220));
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (instanceGuard != null) instanceGuard.close();
        super.stop();
    }

    public static void main(String[] args) { launch(args); }
}
