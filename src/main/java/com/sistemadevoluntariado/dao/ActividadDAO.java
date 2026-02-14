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
import com.sistemadevoluntariado.model.Actividad;

public class ActividadDAO {

    private static final Logger logger = Logger.getLogger(ActividadDAO.class.getName());

    // ── CREAR ──────────────────────────────────────────────
    public boolean crearActividad(Actividad actividad) {
        String sql = "{CALL sp_crear_actividad(?, ?, ?, ?, ?, ?, ?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setString(1, actividad.getNombre());
                cs.setString(2, actividad.getDescripcion());
                
                // Convertir fechas String a java.sql.Date para MySQL
                if (actividad.getFechaInicio() != null && !actividad.getFechaInicio().isEmpty()) {
                    cs.setDate(3, java.sql.Date.valueOf(actividad.getFechaInicio()));
                } else {
                    cs.setNull(3, java.sql.Types.DATE);
                }
                
                if (actividad.getFechaFin() != null && !actividad.getFechaFin().isEmpty()) {
                    cs.setDate(4, java.sql.Date.valueOf(actividad.getFechaFin()));
                } else {
                    cs.setNull(4, java.sql.Types.DATE);
                }
                
                cs.setString(5, actividad.getUbicacion());
                cs.setInt(6, actividad.getCupoMaximo());
                if (actividad.getIdUsuario() > 0) {
                    cs.setInt(7, actividad.getIdUsuario());
                } else {
                    cs.setNull(7, java.sql.Types.INTEGER);
                }
                cs.execute();
                logger.info("✓ Actividad creada correctamente: " + actividad.getNombre());
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al crear actividad: " + e.getMessage(), e);
        }
        return false;
    }

    // ── LISTAR TODAS ───────────────────────────────────────
    public List<Actividad> obtenerTodasActividades() {
        List<Actividad> actividades = new ArrayList<>();
        String sql = "{CALL sp_obtener_todas_actividades()}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return actividades;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                try (ResultSet rs = cs.executeQuery()) {
                    while (rs.next()) {
                        Actividad a = new Actividad();
                        a.setIdActividad(rs.getInt("id_actividad"));
                        a.setNombre(rs.getString("nombre"));
                        a.setDescripcion(rs.getString("descripcion"));
                        a.setFechaInicio(rs.getString("fecha_inicio"));
                        a.setFechaFin(rs.getString("fecha_fin"));
                        a.setUbicacion(rs.getString("ubicacion"));
                        a.setCupoMaximo(rs.getInt("cupo_maximo"));
                        a.setInscritos(rs.getInt("inscritos"));
                        a.setEstado(rs.getString("estado"));
                        a.setIdUsuario(rs.getInt("id_usuario"));
                        a.setCreadoEn(rs.getString("creado_en"));
                        actividades.add(a);
                    }
                }
                logger.info("✓ Se obtuvieron " + actividades.size() + " actividades");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener actividades", e);
        }
        return actividades;
    }

    // ── OBTENER POR ID ─────────────────────────────────────
    public Actividad obtenerActividadPorId(int idActividad) {
        String sql = "{CALL sp_obtener_actividad_por_id(?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return null;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idActividad);
                try (ResultSet rs = cs.executeQuery()) {
                    if (rs.next()) {
                        Actividad a = new Actividad();
                        a.setIdActividad(rs.getInt("id_actividad"));
                        a.setNombre(rs.getString("nombre"));
                        a.setDescripcion(rs.getString("descripcion"));
                        a.setFechaInicio(rs.getString("fecha_inicio"));
                        a.setFechaFin(rs.getString("fecha_fin"));
                        a.setUbicacion(rs.getString("ubicacion"));
                        a.setCupoMaximo(rs.getInt("cupo_maximo"));
                        a.setInscritos(rs.getInt("inscritos"));
                        a.setEstado(rs.getString("estado"));
                        a.setIdUsuario(rs.getInt("id_usuario"));
                        a.setCreadoEn(rs.getString("creado_en"));
                        logger.info("✓ Actividad obtenida: " + a.getNombre());
                        return a;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener actividad", e);
        }
        return null;
    }

    // ── ACTUALIZAR ─────────────────────────────────────────
    public boolean actualizarActividad(Actividad actividad) {
        String sql = "{CALL sp_actualizar_actividad(?, ?, ?, ?, ?, ?, ?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, actividad.getIdActividad());
                cs.setString(2, actividad.getNombre());
                cs.setString(3, actividad.getDescripcion());
                cs.setString(4, actividad.getFechaInicio());
                cs.setString(5, actividad.getFechaFin());
                cs.setString(6, actividad.getUbicacion());
                cs.setInt(7, actividad.getCupoMaximo());
                cs.execute();
                logger.info("✓ Actividad actualizada correctamente: " + actividad.getNombre());
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al actualizar actividad", e);
        }
        return false;
    }

    // ── CAMBIAR ESTADO ─────────────────────────────────────
    public boolean cambiarEstado(int idActividad, String nuevoEstado) {
        String sql = "{CALL sp_cambiar_estado_actividad(?, ?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idActividad);
                cs.setString(2, nuevoEstado);
                cs.execute();
                logger.info("✓ Estado de actividad actualizado a: " + nuevoEstado);
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al cambiar estado de actividad", e);
        }
        return false;
    }

    // ── ELIMINAR ───────────────────────────────────────────
    public boolean eliminarActividad(int idActividad) {
        String sql = "{CALL sp_eliminar_actividad(?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idActividad);
                cs.execute();
                logger.info("✓ Actividad eliminada correctamente");
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al eliminar actividad", e);
        }
        return false;
    }
}
