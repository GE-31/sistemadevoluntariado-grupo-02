package com.sistemadevoluntariado.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.ConexionBD;
import com.sistemadevoluntariado.model.Voluntario;

public class VoluntarioDAO {

    private static final Logger logger = Logger.getLogger(VoluntarioDAO.class.getName());

    /**
     * Crear un nuevo voluntario usando procedimiento almacenado
     */
    public boolean crearVoluntario(Voluntario voluntario) {
        String sql = "{CALL sp_crear_voluntario(?, ?, ?, ?, ?, ?, ?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setString(1, voluntario.getNombres());
                cs.setString(2, voluntario.getApellidos());
                cs.setString(3, voluntario.getDni() != null ? voluntario.getDni() : "");
                cs.setString(4, voluntario.getCorreo());
                cs.setString(5, voluntario.getTelefono());
                cs.setString(6, voluntario.getCarrera());

                // Permitir id_usuario NULL
                if (voluntario.getIdUsuario() > 0) {
                    cs.setInt(7, voluntario.getIdUsuario());
                } else {
                    cs.setNull(7, java.sql.Types.INTEGER);
                }

                cs.execute();
                logger.info("✓ Voluntario creado correctamente: " + voluntario.getNombres());
                return true;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al crear voluntario: " + e.getMessage(), e);
        }

        return false;
    }

    /**
     * Obtener todos los voluntarios usando procedimiento almacenado
     */
    public List<Voluntario> obtenerTodosVoluntarios() {
        List<Voluntario> voluntarios = new ArrayList<>();
        String sql = "{CALL sp_obtener_todos_voluntarios()}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return voluntarios;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                try (ResultSet rs = cs.executeQuery()) {
                    while (rs.next()) {
                        Voluntario v = new Voluntario();
                        v.setIdVoluntario(rs.getInt("id_voluntario"));
                        v.setNombres(rs.getString("nombres"));
                        v.setApellidos(rs.getString("apellidos"));
                        v.setDni(rs.getString("dni"));
                        v.setCorreo(rs.getString("correo"));
                        v.setTelefono(rs.getString("telefono"));
                        v.setCarrera(rs.getString("carrera"));
                        v.setEstado(rs.getString("estado"));
                        v.setIdUsuario(rs.getInt("id_usuario"));
                        
                        voluntarios.add(v);
                    }
                }
                logger.info("✓ Se obtuvieron " + voluntarios.size() + " voluntarios");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener voluntarios", e);
        }

        return voluntarios;
    }

    /**
     * Obtener un voluntario por ID usando procedimiento almacenado
     */
    public Voluntario obtenerVoluntarioPorId(int idVoluntario) {
        String sql = "{CALL sp_obtener_voluntario_por_id(?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return null;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idVoluntario);

                try (ResultSet rs = cs.executeQuery()) {
                    if (rs.next()) {
                        Voluntario v = new Voluntario();
                        v.setIdVoluntario(rs.getInt("id_voluntario"));
                        v.setNombres(rs.getString("nombres"));
                        v.setApellidos(rs.getString("apellidos"));
                        v.setDni(rs.getString("dni"));
                        v.setCorreo(rs.getString("correo"));
                        v.setTelefono(rs.getString("telefono"));
                        v.setCarrera(rs.getString("carrera"));
                        v.setEstado(rs.getString("estado"));
                        v.setIdUsuario(rs.getInt("id_usuario"));
                        
                        logger.info("✓ Voluntario obtenido: " + v.getNombres());
                        return v;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener voluntario", e);
        }

        return null;
    }

    /**
     * Actualizar voluntario usando procedimiento almacenado
     */
    public boolean actualizarVoluntario(Voluntario voluntario) {
        String sql = "{CALL sp_actualizar_voluntario(?, ?, ?, ?, ?, ?, ?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, voluntario.getIdVoluntario());
                cs.setString(2, voluntario.getNombres());
                cs.setString(3, voluntario.getApellidos());
                cs.setString(4, voluntario.getDni() != null ? voluntario.getDni() : "");
                cs.setString(5, voluntario.getCorreo());
                cs.setString(6, voluntario.getTelefono());
                cs.setString(7, voluntario.getCarrera());

                cs.execute();
                logger.info("✓ Voluntario actualizado correctamente: " + voluntario.getNombres());
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al actualizar voluntario", e);
        }

        return false;
    }

    /**
     * Cambiar estado del voluntario usando procedimiento almacenado
     */
    public boolean cambiarEstado(int idVoluntario, String nuevoEstado) {
        String sql = "{CALL sp_cambiar_estado_voluntario(?, ?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idVoluntario);
                cs.setString(2, nuevoEstado);

                cs.execute();
                logger.info("✓ Estado del voluntario actualizado a: " + nuevoEstado);
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al cambiar estado del voluntario", e);
        }

        return false;
    }

    /**
     * Eliminar voluntario usando procedimiento almacenado
     */
    public boolean eliminarVoluntario(int idVoluntario) {
        String sql = "{CALL sp_eliminar_voluntario(?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idVoluntario);

                cs.execute();
                logger.info("✓ Voluntario eliminado correctamente");
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al eliminar voluntario", e);
        }

        return false;
    }
}
