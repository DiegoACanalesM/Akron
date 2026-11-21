package com.negocio.analitica.security;

import com.negocio.analitica.database.ConnectionProvider;
import com.negocio.analitica.model.Usuario;
import com.negocio.analitica.repository.PermissionRepository;
import com.negocio.analitica.repository.UserAdministrationRepository;
import com.negocio.analitica.repository.UsuarioRepository;
import com.negocio.analitica.repository.postgresql.PostgresPermissionRepository;
import com.negocio.analitica.repository.postgresql.PostgresUserAdministrationRepository;
import com.negocio.analitica.repository.postgresql.PostgresUsuarioRepository;
import com.negocio.analitica.service.AuthenticationService;
import com.negocio.analitica.service.UserManagementService;

import java.io.Console;
import java.util.Optional;
import java.util.Scanner;

/** Console first-run setup and login flow backed by PostgreSQL. */
public final class ConsoleAuthenticationFlow {
    private ConsoleAuthenticationFlow() { }

    public static Optional<AuthenticatedSession> authenticate(
            Scanner scanner, ConnectionProvider connections) {
        if (scanner == null || connections == null) {
            throw new IllegalArgumentException("Scanner and database connection are required.");
        }

        UsuarioRepository users = new PostgresUsuarioRepository(connections);
        PermissionRepository permissions = new PostgresPermissionRepository(connections);
        UserAdministrationRepository administration =
                new PostgresUserAdministrationRepository(connections);
        PasswordHasher hasher = new BCryptPasswordHasher();
        UserManagementService userManagement = new UserManagementService(administration, hasher);

        if (!administration.hasAnyUser()) {
            System.out.println("\n=== CONFIGURACIÓN INICIAL DE AKRON ===");
            System.out.println("No hay usuarios registrados. Vamos a crear la cuenta del propietario.");
            String username = readRequired(scanner, "Nombre de usuario: ");
            String fullName = readRequired(scanner, "Nombre completo: ");
            char[] password = readPassword(scanner, "Crea una contraseña (mínimo 10 caracteres): ");
            char[] confirmation = readPassword(scanner, "Confirma la contraseña: ");
            try {
                if (!java.util.Arrays.equals(password, confirmation)) {
                    System.out.println("Las contraseñas no coinciden. Ejecuta AKRON nuevamente para repetir el proceso.");
                    return Optional.empty();
                }
                userManagement.bootstrapOwner(username, fullName, password);
                System.out.println("Cuenta OWNER creada correctamente.");
            } finally {
                clear(password);
                clear(confirmation);
            }
        }

        AuthenticationService authentication =
                new AuthenticationService(users, permissions, hasher);
        for (int attempt = 1; attempt <= 5; attempt++) {
            System.out.println("\n=== INICIO DE SESIÓN AKRON ===");
            String username = readRequired(scanner, "Usuario: ");
            char[] password = readPassword(scanner, "Contraseña: ");
            Optional<AuthenticatedSession> session = authentication.login(username, password);
            if (session.isPresent()) {
                System.out.println("Bienvenido, " + session.get().getFullName() + ".");
                return session;
            }
            System.out.println("Credenciales incorrectas o cuenta inactiva. Intentos restantes: "
                    + (5 - attempt));
        }
        System.out.println("Se alcanzó el límite de intentos. La aplicación se cerrará.");
        return Optional.empty();
    }

    public static char[] readPassword(Scanner scanner, String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] password = console.readPassword("%s", prompt);
            return password == null ? new char[0] : password;
        }
        System.out.print(prompt);
        System.out.println("(Aviso: esta terminal no permite ocultar lo que escribes.)");
        return scanner.nextLine().toCharArray();
    }

    private static String readRequired(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) return value;
            System.out.println("Este campo es obligatorio.");
        }
    }

    private static void clear(char[] value) {
        if (value != null) java.util.Arrays.fill(value, '\0');
    }
}
