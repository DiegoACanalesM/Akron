package com.negocio.analitica.database;

import java.sql.Connection;
import java.sql.SQLException;

/** Supplies database connections to PostgreSQL repositories. */
@FunctionalInterface
public interface ConnectionProvider {
    Connection getConnection() throws SQLException;
}
