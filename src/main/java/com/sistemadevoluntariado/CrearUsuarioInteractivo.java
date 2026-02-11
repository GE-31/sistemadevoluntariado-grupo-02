package com.sistemadevoluntariado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

import org.mindrot.jbcrypt.BCrypt;

import com.sistemadevoluntariado.config.ConexionBD;

public class CrearUsuarioInteractivo {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("==========================================");
        System.out.println("   CREAR NUEVO USUARIO");
        System.out.println("==========================================\n");
        
        try {
            // Solicitar datos
            System.out.print("Ingresa el nombre: ");
            String nombres = scanner.nextLine().trim();
            
            System.out.print("Ingresa los apellidos: ");
            String apellidos = scanner.nextLine().trim();
            
            System.out.print("Ingresa el correo: ");
            String correo = scanner.nextLine().trim();
            
            System.out.print("Ingresa el username: ");
            String username = scanner.nextLine().trim();
            
            System.out.print("Ingresa la contraseña: ");
            String password = scanner.nextLine().trim();
            
            // Validar datos
            if (nombres.isEmpty() || apellidos.isEmpty() || correo.isEmpty() || 
                username.isEmpty() || password.isEmpty()) {
                System.out.println("\n❌ Error: Todos los campos son obligatorios");
                scanner.close();
                return;
            }
            
            // Generar hash BCrypt
            System.out.println("\n⏳ Generando hash de contraseña...");
            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
            System.out.println("✓ Hash generado correctamente");
            
            // Insertar en base de datos
            System.out.println("⏳ Insertando usuario en la base de datos...");
            insertarUsuario(nombres, apellidos, correo, username, passwordHash);
            
            System.out.println("\n==========================================");
            System.out.println("✓ ¡USUARIO CREADO EXITOSAMENTE!");
            System.out.println("==========================================");
            System.out.println("Detalles:");
            System.out.println("  Nombre: " + nombres + " " + apellidos);
            System.out.println("  Username: " + username);
            System.out.println("  Correo: " + correo);
            System.out.println("  Estado: ACTIVO");
            System.out.println("\nAhora puede iniciar sesión en la aplicación.");
            System.out.println("==========================================\n");
            
        } catch (Exception e) {
            System.out.println("\n❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    private static void insertarUsuario(String nombres, String apellidos, String correo, 
                                       String username, String passwordHash) throws SQLException {
        ConexionBD conexionBD = ConexionBD.getInstance();
        Connection conexion = conexionBD.getConnection();
        
        if (conexion == null) {
            throw new SQLException("No se pudo establecer conexión con la base de datos");
        }
        
        String sql = "INSERT INTO usuario (nombres, apellidos, correo, username, password_hash, estado, creado_en, actualizado_en) " +
                    "VALUES (?, ?, ?, ?, ?, 'ACTIVO', NOW(), NOW())";
        
        PreparedStatement pstmt = conexion.prepareStatement(sql);
        pstmt.setString(1, nombres);
        pstmt.setString(2, apellidos);
        pstmt.setString(3, correo);
        pstmt.setString(4, username);
        pstmt.setString(5, passwordHash);
        
        int filasInsertadas = pstmt.executeUpdate();
        
        pstmt.close();
        conexion.close();
        
        if (filasInsertadas == 0) {
            throw new SQLException("No se pudo insertar el usuario");
        }
    }
}
