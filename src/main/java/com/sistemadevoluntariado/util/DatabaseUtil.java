package com.sistemadevoluntariado.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utilidad de base de datos.
 * Expone el DataSource de Spring de forma estática para que los
 * Repository (instanciados con "new") puedan obtener conexiones JDBC.
 */
@Component
public class DatabaseUtil {

    private static final Logger logger = Logger.getLogger(DatabaseUtil.class.getName());

    private static DataSource dataSource;

    @Autowired
    public void setDataSource(DataSource ds) {
        DatabaseUtil.dataSource = ds;
        logger.info("✓ DatabaseUtil: DataSource inicializado.");
    }

    /**
     * Retorna una conexión JDBC del pool de Spring.
     * Siempre cerrar con try-with-resources o en finally.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource no inicializado. ¿Se inició Spring Boot correctamente?");
        }
        return dataSource.getConnection();
    }

    /** Cierra silenciosamente una conexión. */
    public static void close(Connection conn) {
        if (conn != null) {
            try { conn.close(); } catch (SQLException e) {
                logger.log(Level.WARNING, "Error al cerrar conexión", e);
            }
        }
    }
}
