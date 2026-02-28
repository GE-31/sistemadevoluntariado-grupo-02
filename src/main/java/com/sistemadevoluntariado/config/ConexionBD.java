package com.sistemadevoluntariado.config;

import java.sql.Connection;
import java.sql.SQLException;

import com.sistemadevoluntariado.util.DatabaseUtil;

/**
 * Provee acceso a conexiones JDBC mediante patrón Singleton.
 * Delega a DatabaseUtil (que usa el DataSource de Spring).
 */
public class ConexionBD {

    private static final ConexionBD INSTANCE = new ConexionBD();

    private ConexionBD() {}

    public static ConexionBD getInstance() {
        return INSTANCE;
    }

    /** Retorna una conexión del pool. SIEMPRE cerrar con try-with-resources. */
    public Connection getConnection() throws RuntimeException {
        try {
            return DatabaseUtil.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener conexión JDBC", e);
        }
    }

    /** Método estático alternativo para compatibilidad con DatabaseUtil. */
    public static Connection getConexion() {
        return INSTANCE.getConnection();
    }
}
