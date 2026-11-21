package com.negocio.analitica.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** PostgreSQL connection provider configured through environment variables. */
public final class PostgresConnectionProvider implements ConnectionProvider {
    private final String url;
    private final String username;
    private final String password;

    public PostgresConnectionProvider(String url, String username, String password) {
        if (url == null || !url.startsWith("jdbc:postgresql://")
                || username == null || username.isBlank()
                || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La configuración de PostgreSQL está incompleta.");
        }
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public static PostgresConnectionProvider fromEnvironment() {
        String url = System.getenv().getOrDefault(
                "AKRON_DB_URL", "jdbc:postgresql://localhost:5432/akron_db");
        String username = System.getenv("AKRON_DB_USER");
        String password = System.getenv("AKRON_DB_PASSWORD");
        return new PostgresConnectionProvider(url, username, password);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
