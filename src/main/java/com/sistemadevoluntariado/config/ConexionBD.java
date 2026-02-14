package com.sistemadevoluntariado.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConexionBD {

    private static final Logger logger = Logger.getLogger(ConexionBD.class.getName());

    private static final String URL = "jdbc:mysql://localhost:3306/sistema_voluntariado?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    private Connection connection;

    // Constructor privado
    private ConexionBD() {
        try {
            Class.forName(DRIVER);
            this.connection = DriverManager.getConnection(URL, USER, PASS);
            logger.info("Conexión exitosa a la base de datos");
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "No se encontró el driver MySQL", e);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al conectar a la base de datos", e);
        }
    }

    // Singleton usando Holder Pattern
    private static class Holder {
        private static final ConexionBD INSTANCE = new ConexionBD();
    }

    // Obtener instancia
    public static ConexionBD getInstance() {
        return Holder.INSTANCE;
    }

    // Obtener conexión
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASS);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener la conexión", e);
        }
        return connection;
    }

    // Cerrar conexión
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Conexión cerrada correctamente");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al cerrar la conexión", e);
        }
    }

    // Probar conexión
    public boolean testConnection() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error al probar la conexión", e);
            return false;
        }
    }
}

