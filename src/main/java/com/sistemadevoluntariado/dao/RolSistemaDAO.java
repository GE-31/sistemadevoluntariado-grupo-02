package com.sistemadevoluntariado.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.sistemadevoluntariado.model.RolSistema;
import com.sistemadevoluntariado.config.ConexionBD;

public class RolSistemaDAO {

    /**
     * Obtener todos los roles del sistema
     */
    public List<RolSistema> obtenerTodosRoles() {
        List<RolSistema> roles = new ArrayList<>();
        String sql = "SELECT id_rol_sistema, nombre_rol, descripcion FROM rol_sistema ORDER BY nombre_rol";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                System.err.println("✗ No se pudo establecer conexión a la base de datos");
                return roles;
            }

            try (CallableStatement stmt = conn.prepareCall(sql);
                    ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    RolSistema rol = new RolSistema();
                    rol.setIdRolSistema(rs.getInt("id_rol_sistema"));
                    rol.setNombreRol(rs.getString("nombre_rol"));
                    rol.setDescripcion(rs.getString("descripcion"));
                    roles.add(rol);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return roles;
    }

    /**
     * Obtener un rol por ID
     */
    public RolSistema obtenerRolPorId(int idRol) {
        RolSistema rol = null;
        String sql = "SELECT id_rol_sistema, nombre_rol, descripcion FROM rol_sistema WHERE id_rol_sistema = ?";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                System.err.println("✗ No se pudo establecer conexión a la base de datos");
                return null;
            }

            try (CallableStatement stmt = conn.prepareCall(sql)) {
                stmt.setInt(1, idRol);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        rol = new RolSistema();
                        rol.setIdRolSistema(rs.getInt("id_rol_sistema"));
                        rol.setNombreRol(rs.getString("nombre_rol"));
                        rol.setDescripcion(rs.getString("descripcion"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rol;
    }
}
