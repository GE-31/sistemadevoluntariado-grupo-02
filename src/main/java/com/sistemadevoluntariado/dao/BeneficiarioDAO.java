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
import com.sistemadevoluntariado.model.Beneficiario;

public class BeneficiarioDAO {

    private static final Logger logger = Logger.getLogger(BeneficiarioDAO.class.getName());

    // ── CREAR ──────────────────────────────────────────────
    public boolean crearBeneficiario(Beneficiario b) {
        String sql = "{CALL sp_crear_beneficiario(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setString(1, b.getNombres());
                cs.setString(2, b.getApellidos());
                cs.setString(3, b.getDni());
                cs.setString(4, b.getFechaNacimiento());
                cs.setString(5, b.getTelefono());
                cs.setString(6, b.getDireccion());
                cs.setString(7, b.getDistrito());
                cs.setString(8, b.getTipoBeneficiario());
                cs.setString(9, b.getNecesidadPrincipal());
                cs.setString(10, b.getObservaciones());
                if (b.getIdUsuario() > 0) {
                    cs.setInt(11, b.getIdUsuario());
                } else {
                    cs.setNull(11, java.sql.Types.INTEGER);
                }
                cs.execute();
                logger.info("✓ Beneficiario creado correctamente: " + b.getNombres() + " " + b.getApellidos());
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al crear beneficiario: " + e.getMessage(), e);
        }
        return false;
    }

    // ── LISTAR TODOS ───────────────────────────────────────
    public List<Beneficiario> obtenerTodosBeneficiarios() {
        List<Beneficiario> beneficiarios = new ArrayList<>();
        String sql = "{CALL sp_obtener_todos_beneficiarios()}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return beneficiarios;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                try (ResultSet rs = cs.executeQuery()) {
                    while (rs.next()) {
                        Beneficiario b = new Beneficiario();
                        b.setIdBeneficiario(rs.getInt("id_beneficiario"));
                        b.setNombres(rs.getString("nombres"));
                        b.setApellidos(rs.getString("apellidos"));
                        b.setDni(rs.getString("dni"));
                        b.setFechaNacimiento(rs.getString("fecha_nacimiento"));
                        b.setTelefono(rs.getString("telefono"));
                        b.setDireccion(rs.getString("direccion"));
                        b.setDistrito(rs.getString("distrito"));
                        b.setTipoBeneficiario(rs.getString("tipo_beneficiario"));
                        b.setNecesidadPrincipal(rs.getString("necesidad_principal"));
                        b.setObservaciones(rs.getString("observaciones"));
                        b.setEstado(rs.getString("estado"));
                        b.setIdUsuario(rs.getInt("id_usuario"));
                        b.setCreadoEn(rs.getString("creado_en"));
                        beneficiarios.add(b);
                    }
                }
                logger.info("✓ Se obtuvieron " + beneficiarios.size() + " beneficiarios");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener beneficiarios", e);
        }
        return beneficiarios;
    }

    // ── OBTENER POR ID ─────────────────────────────────────
    public Beneficiario obtenerBeneficiarioPorId(int idBeneficiario) {
        String sql = "{CALL sp_obtener_beneficiario_por_id(?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return null;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idBeneficiario);
                try (ResultSet rs = cs.executeQuery()) {
                    if (rs.next()) {
                        Beneficiario b = new Beneficiario();
                        b.setIdBeneficiario(rs.getInt("id_beneficiario"));
                        b.setNombres(rs.getString("nombres"));
                        b.setApellidos(rs.getString("apellidos"));
                        b.setDni(rs.getString("dni"));
                        b.setFechaNacimiento(rs.getString("fecha_nacimiento"));
                        b.setTelefono(rs.getString("telefono"));
                        b.setDireccion(rs.getString("direccion"));
                        b.setDistrito(rs.getString("distrito"));
                        b.setTipoBeneficiario(rs.getString("tipo_beneficiario"));
                        b.setNecesidadPrincipal(rs.getString("necesidad_principal"));
                        b.setObservaciones(rs.getString("observaciones"));
                        b.setEstado(rs.getString("estado"));
                        b.setIdUsuario(rs.getInt("id_usuario"));
                        b.setCreadoEn(rs.getString("creado_en"));
                        logger.info("✓ Beneficiario obtenido: " + b.getNombres());
                        return b;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener beneficiario", e);
        }
        return null;
    }

    // ── ACTUALIZAR ─────────────────────────────────────────
    public boolean actualizarBeneficiario(Beneficiario b) {
        String sql = "{CALL sp_actualizar_beneficiario(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, b.getIdBeneficiario());
                cs.setString(2, b.getNombres());
                cs.setString(3, b.getApellidos());
                cs.setString(4, b.getDni());
                cs.setString(5, b.getFechaNacimiento());
                cs.setString(6, b.getTelefono());
                cs.setString(7, b.getDireccion());
                cs.setString(8, b.getDistrito());
                cs.setString(9, b.getTipoBeneficiario());
                cs.setString(10, b.getNecesidadPrincipal());
                cs.setString(11, b.getObservaciones());
                cs.execute();
                logger.info("✓ Beneficiario actualizado correctamente: " + b.getNombres());
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al actualizar beneficiario", e);
        }
        return false;
    }

    // ── CAMBIAR ESTADO ─────────────────────────────────────
    public boolean cambiarEstado(int idBeneficiario, String nuevoEstado) {
        String sql = "{CALL sp_cambiar_estado_beneficiario(?, ?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idBeneficiario);
                cs.setString(2, nuevoEstado);
                cs.execute();
                logger.info("✓ Estado de beneficiario actualizado a: " + nuevoEstado);
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al cambiar estado de beneficiario", e);
        }
        return false;
    }

    // ── ELIMINAR ───────────────────────────────────────────
    public boolean eliminarBeneficiario(int idBeneficiario) {
        String sql = "{CALL sp_eliminar_beneficiario(?)}";
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();
            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idBeneficiario);
                cs.execute();
                logger.info("✓ Beneficiario eliminado correctamente");
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al eliminar beneficiario", e);
        }
        return false;
    }
}
