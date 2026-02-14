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
import com.sistemadevoluntariado.model.Certificado;

public class CertificadoDAO {

    private static final Logger logger = Logger.getLogger(CertificadoDAO.class.getName());

    // ── CREAR CERTIFICADO ──────────────────────────────────
    public boolean crearCertificado(Certificado c) {
        String sql = "{CALL sp_crear_certificado(?, ?, ?, ?, ?, ?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setString(1, null); // código auto-generado
                cs.setInt(2, c.getIdVoluntario());
                cs.setInt(3, c.getIdActividad());
                cs.setInt(4, c.getHorasVoluntariado());
                cs.setString(5, c.getObservaciones());
                cs.setInt(6, c.getIdUsuarioEmite());
                cs.execute();
                logger.info("✓ Certificado creado correctamente");
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al crear certificado: " + e.getMessage(), e);
        }
        return false;
    }

    // ── LISTAR TODOS ───────────────────────────────────────
    public List<Certificado> obtenerTodosCertificados() {
        List<Certificado> certificados = new ArrayList<>();
        String sql = "{CALL sp_listar_certificados()}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return certificados;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                try (ResultSet rs = cs.executeQuery()) {
                    while (rs.next()) {
                        Certificado c = new Certificado();
                        c.setIdCertificado(rs.getInt("id_certificado"));
                        c.setCodigoCertificado(rs.getString("codigo_certificado"));
                        c.setIdVoluntario(rs.getInt("id_voluntario"));
                        c.setIdActividad(rs.getInt("id_actividad"));
                        c.setHorasVoluntariado(rs.getInt("horas_voluntariado"));
                        c.setFechaEmision(rs.getString("fecha_emision"));
                        c.setEstado(rs.getString("estado"));
                        c.setObservaciones(rs.getString("observaciones"));
                        c.setNombreVoluntario(rs.getString("nombre_voluntario"));
                        c.setDniVoluntario(rs.getString("dni_voluntario"));
                        c.setNombreActividad(rs.getString("nombre_actividad"));
                        c.setUsuarioEmite(rs.getString("usuario_emite"));
                        certificados.add(c);
                    }
                }
                logger.info("✓ Se obtuvieron " + certificados.size() + " certificados");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener certificados", e);
        }
        return certificados;
    }

    // ── OBTENER POR ID ─────────────────────────────────────
    public Certificado obtenerCertificadoPorId(int id) {
        String sql = "{CALL sp_obtener_certificado_por_id(?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null)
                return null;

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, id);
                try (ResultSet rs = cs.executeQuery()) {
                    if (rs.next()) {
                        Certificado c = new Certificado();
                        c.setIdCertificado(rs.getInt("id_certificado"));
                        c.setCodigoCertificado(rs.getString("codigo_certificado"));
                        c.setIdVoluntario(rs.getInt("id_voluntario"));
                        c.setIdActividad(rs.getInt("id_actividad"));
                        c.setHorasVoluntariado(rs.getInt("horas_voluntariado"));
                        c.setFechaEmision(rs.getString("fecha_emision"));
                        c.setEstado(rs.getString("estado"));
                        c.setObservaciones(rs.getString("observaciones"));
                        c.setNombreVoluntario(rs.getString("nombre_voluntario"));
                        c.setDniVoluntario(rs.getString("dni_voluntario"));
                        c.setNombreActividad(rs.getString("nombre_actividad"));
                        c.setUsuarioEmite(rs.getString("usuario_emite"));
                        return c;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener certificado por ID", e);
        }
        return null;
    }

    // ── OBTENER POR CÓDIGO ─────────────────────────────────
    public Certificado obtenerCertificadoPorCodigo(String codigo) {
        String sql = "{CALL sp_obtener_certificado_por_codigo(?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null)
                return null;

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setString(1, codigo);
                try (ResultSet rs = cs.executeQuery()) {
                    if (rs.next()) {
                        Certificado c = new Certificado();
                        c.setIdCertificado(rs.getInt("id_certificado"));
                        c.setCodigoCertificado(rs.getString("codigo_certificado"));
                        c.setIdVoluntario(rs.getInt("id_voluntario"));
                        c.setIdActividad(rs.getInt("id_actividad"));
                        c.setHorasVoluntariado(rs.getInt("horas_voluntariado"));
                        c.setFechaEmision(rs.getString("fecha_emision"));
                        c.setEstado(rs.getString("estado"));
                        c.setObservaciones(rs.getString("observaciones"));
                        c.setNombreVoluntario(rs.getString("nombre_voluntario"));
                        c.setDniVoluntario(rs.getString("dni_voluntario"));
                        c.setNombreActividad(rs.getString("nombre_actividad"));
                        c.setUsuarioEmite(rs.getString("usuario_emite"));
                        return c;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener certificado por código", e);
        }
        return null;
    }

    // ── ANULAR CERTIFICADO ─────────────────────────────────
    public boolean anularCertificado(int id, String motivo) {
        String sql = "{CALL sp_anular_certificado(?, ?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null)
                return false;

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, id);
                cs.setString(2, motivo);
                cs.execute();
                logger.info("✓ Certificado anulado correctamente");
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al anular certificado", e);
        }
        return false;
    }

    // ── OBTENER CERTIFICADOS POR VOLUNTARIO ────────────────
    public List<Certificado> obtenerCertificadosPorVoluntario(int idVoluntario) {
        List<Certificado> certificados = new ArrayList<>();
        String sql = "{CALL sp_certificados_por_voluntario(?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null)
                return certificados;

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idVoluntario);
                try (ResultSet rs = cs.executeQuery()) {
                    while (rs.next()) {
                        Certificado c = new Certificado();
                        c.setIdCertificado(rs.getInt("id_certificado"));
                        c.setCodigoCertificado(rs.getString("codigo_certificado"));
                        c.setHorasVoluntariado(rs.getInt("horas_voluntariado"));
                        c.setFechaEmision(rs.getString("fecha_emision"));
                        c.setEstado(rs.getString("estado"));
                        c.setNombreActividad(rs.getString("nombre_actividad"));
                        certificados.add(c);
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener certificados por voluntario", e);
        }
        return certificados;
    }
}
