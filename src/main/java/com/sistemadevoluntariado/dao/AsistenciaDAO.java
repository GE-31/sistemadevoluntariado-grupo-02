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
import com.sistemadevoluntariado.model.Asistencia;

public class AsistenciaDAO {

    private static final Logger logger = Logger.getLogger(AsistenciaDAO.class.getName());

    /**
     * Registrar nueva asistencia usando procedimiento almacenado
     */
    public boolean registrarAsistencia(Asistencia asistencia) {
        String sql = "{CALL sp_registrar_asistencia(?, ?, ?, ?, ?, ?, ?, ?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, asistencia.getIdVoluntario());
                cs.setInt(2, asistencia.getIdActividad());
                cs.setString(3, asistencia.getFecha());

                if (asistencia.getHoraEntrada() != null && !asistencia.getHoraEntrada().isEmpty()) {
                    cs.setString(4, asistencia.getHoraEntrada());
                } else {
                    cs.setNull(4, java.sql.Types.TIME);
                }

                if (asistencia.getHoraSalida() != null && !asistencia.getHoraSalida().isEmpty()) {
                    cs.setString(5, asistencia.getHoraSalida());
                } else {
                    cs.setNull(5, java.sql.Types.TIME);
                }

                cs.setString(6, asistencia.getEstado());

                if (asistencia.getObservaciones() != null && !asistencia.getObservaciones().isEmpty()) {
                    cs.setString(7, asistencia.getObservaciones());
                } else {
                    cs.setNull(7, java.sql.Types.VARCHAR);
                }

                if (asistencia.getIdUsuarioRegistro() > 0) {
                    cs.setInt(8, asistencia.getIdUsuarioRegistro());
                } else {
                    cs.setNull(8, java.sql.Types.INTEGER);
                }

                cs.execute();
                logger.info("✓ Asistencia registrada correctamente");
                return true;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al registrar asistencia: " + e.getMessage(), e);
        }

        return false;
    }

    /**
     * Listar todas las asistencias
     */
    public List<Asistencia> listarAsistencias() {
        List<Asistencia> lista = new ArrayList<>();
        String sql = "{CALL sp_listar_asistencias()}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return lista;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                try (ResultSet rs = cs.executeQuery()) {
                    while (rs.next()) {
                        lista.add(mapearAsistencia(rs));
                    }
                }
                logger.info("✓ Se obtuvieron " + lista.size() + " asistencias");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al listar asistencias", e);
        }

        return lista;
    }

    /**
     * Obtener asistencia por ID
     */
    public Asistencia obtenerPorId(int idAsistencia) {
        String sql = "{CALL sp_obtener_asistencia_por_id(?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return null;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idAsistencia);
                try (ResultSet rs = cs.executeQuery()) {
                    if (rs.next()) {
                        return mapearAsistencia(rs);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener asistencia por ID", e);
        }

        return null;
    }

    /**
     * Listar asistencias por actividad
     */
    public List<Asistencia> listarPorActividad(int idActividad) {
        List<Asistencia> lista = new ArrayList<>();
        String sql = "{CALL sp_listar_asistencias_por_actividad(?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return lista;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idActividad);
                try (ResultSet rs = cs.executeQuery()) {
                    while (rs.next()) {
                        lista.add(mapearAsistencia(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al listar asistencias por actividad", e);
        }

        return lista;
    }

    /**
     * Listar asistencias por voluntario
     */
    public List<Asistencia> listarPorVoluntario(int idVoluntario) {
        List<Asistencia> lista = new ArrayList<>();
        String sql = "{CALL sp_listar_asistencias_por_voluntario(?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return lista;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idVoluntario);
                try (ResultSet rs = cs.executeQuery()) {
                    while (rs.next()) {
                        lista.add(mapearAsistencia(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al listar asistencias por voluntario", e);
        }

        return lista;
    }

    /**
     * Actualizar asistencia
     */
    public boolean actualizarAsistencia(Asistencia asistencia) {
        String sql = "{CALL sp_actualizar_asistencia(?, ?, ?, ?, ?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, asistencia.getIdAsistencia());

                if (asistencia.getHoraEntrada() != null && !asistencia.getHoraEntrada().isEmpty()) {
                    cs.setString(2, asistencia.getHoraEntrada());
                } else {
                    cs.setNull(2, java.sql.Types.TIME);
                }

                if (asistencia.getHoraSalida() != null && !asistencia.getHoraSalida().isEmpty()) {
                    cs.setString(3, asistencia.getHoraSalida());
                } else {
                    cs.setNull(3, java.sql.Types.TIME);
                }

                cs.setString(4, asistencia.getEstado());

                if (asistencia.getObservaciones() != null && !asistencia.getObservaciones().isEmpty()) {
                    cs.setString(5, asistencia.getObservaciones());
                } else {
                    cs.setNull(5, java.sql.Types.VARCHAR);
                }

                cs.execute();
                logger.info("✓ Asistencia actualizada correctamente ID: " + asistencia.getIdAsistencia());
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al actualizar asistencia", e);
        }

        return false;
    }

    /**
     * Eliminar asistencia
     */
    public boolean eliminarAsistencia(int idAsistencia) {
        String sql = "{CALL sp_eliminar_asistencia(?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idAsistencia);
                cs.execute();
                logger.info("✓ Asistencia eliminada correctamente ID: " + idAsistencia);
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al eliminar asistencia", e);
        }

        return false;
    }

    /**
     * Mapear ResultSet a objeto Asistencia
     */
    private Asistencia mapearAsistencia(ResultSet rs) throws SQLException {
        Asistencia a = new Asistencia();
        a.setIdAsistencia(rs.getInt("id_asistencia"));
        a.setIdVoluntario(rs.getInt("id_voluntario"));
        a.setNombreVoluntario(rs.getString("nombre_voluntario"));

        try { a.setDniVoluntario(rs.getString("dni_voluntario")); } catch (SQLException ignored) {}

        a.setIdActividad(rs.getInt("id_actividad"));
        a.setNombreActividad(rs.getString("nombre_actividad"));
        a.setFecha(rs.getString("fecha"));

        String horaEntrada = rs.getString("hora_entrada");
        a.setHoraEntrada(horaEntrada != null ? horaEntrada.substring(0, Math.min(5, horaEntrada.length())) : null);

        String horaSalida = rs.getString("hora_salida");
        a.setHoraSalida(horaSalida != null ? horaSalida.substring(0, Math.min(5, horaSalida.length())) : null);

        a.setHorasTotales(rs.getBigDecimal("horas_totales"));
        a.setEstado(rs.getString("estado"));
        a.setObservaciones(rs.getString("observaciones"));

        try { a.setIdUsuarioRegistro(rs.getInt("id_usuario_registro")); } catch (SQLException ignored) {}
        try { a.setUsuarioRegistro(rs.getString("usuario_registro")); } catch (SQLException ignored) {}

        a.setCreadoEn(rs.getString("creado_en"));
        return a;
    }
}
