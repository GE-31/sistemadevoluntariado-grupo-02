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
import com.sistemadevoluntariado.model.Notificacion;

public class NotificacionDAO {

    private static final Logger logger = Logger.getLogger(NotificacionDAO.class.getName());

    // ── CREAR NOTIFICACIÓN ─────────────────────────────────
    public boolean crearNotificacion(Notificacion n) {
        String sql = "{CALL sp_crear_notificacion(?, ?, ?, ?, ?, ?, ?)}";
        try {
            Connection conn = ConexionBD.getInstance().getConnection();
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, n.getIdUsuario());
                cs.setString(2, n.getTipo());
                cs.setString(3, n.getTitulo());
                cs.setString(4, n.getMensaje());
                cs.setString(5, n.getIcono() != null ? n.getIcono() : "fa-bell");
                cs.setString(6, n.getColor() != null ? n.getColor() : "#6366f1");
                if (n.getReferenciaId() > 0) {
                    cs.setInt(7, n.getReferenciaId());
                } else {
                    cs.setNull(7, java.sql.Types.INTEGER);
                }
                cs.execute();
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al crear notificación", e);
            return false;
        }
    }

    // ── LISTAR NOTIFICACIONES POR USUARIO ──────────────────
    public List<Notificacion> listarPorUsuario(int idUsuario) {
        List<Notificacion> lista = new ArrayList<>();
        String sql = "{CALL sp_listar_notificaciones(?)}";
        try {
            Connection conn = ConexionBD.getInstance().getConnection();
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idUsuario);
                try (ResultSet rs = cs.executeQuery()) {
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al listar notificaciones", e);
        }
        return lista;
    }

    // ── CONTAR NO LEÍDAS ───────────────────────────────────
    public int contarNoLeidas(int idUsuario) {
        String sql = "{CALL sp_contar_notificaciones_no_leidas(?)}";
        try {
            Connection conn = ConexionBD.getInstance().getConnection();
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idUsuario);
                try (ResultSet rs = cs.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al contar notificaciones", e);
        }
        return 0;
    }

    // ── MARCAR COMO LEÍDA ──────────────────────────────────
    public boolean marcarLeida(int idNotificacion) {
        String sql = "{CALL sp_marcar_notificacion_leida(?)}";
        try {
            Connection conn = ConexionBD.getInstance().getConnection();
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idNotificacion);
                cs.execute();
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al marcar notificación como leída", e);
            return false;
        }
    }

    // ── MARCAR TODAS COMO LEÍDAS ───────────────────────────
    public boolean marcarTodasLeidas(int idUsuario) {
        String sql = "{CALL sp_marcar_todas_leidas(?)}";
        try {
            Connection conn = ConexionBD.getInstance().getConnection();
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idUsuario);
                cs.execute();
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al marcar todas como leídas", e);
            return false;
        }
    }

    // ── GENERAR NOTIFICACIONES DE ACTIVIDADES DE HOY ───────
    public void generarNotificacionesActividadesHoy(int idUsuario) {
        String sql = "{CALL sp_generar_notificaciones_actividades_hoy(?)}";
        try {
            Connection conn = ConexionBD.getInstance().getConnection();
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idUsuario);
                cs.execute();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al generar notificaciones de actividades", e);
        }
    }

    // ── GENERAR NOTIFICACIONES DE EVENTOS DEL CALENDARIO HOY ─
    public void generarNotificacionesEventosHoy(int idUsuario) {
        String sql = "{CALL sp_generar_notificaciones_eventos_hoy(?)}";
        try {
            Connection conn = ConexionBD.getInstance().getConnection();
            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idUsuario);
                cs.execute();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al generar notificaciones de eventos", e);
        }
    }

    // ── MAPEAR RESULTSET A NOTIFICACIÓN ────────────────────
    private Notificacion mapear(ResultSet rs) throws SQLException {
        Notificacion n = new Notificacion();
        n.setIdNotificacion(rs.getInt("id_notificacion"));
        n.setIdUsuario(rs.getInt("id_usuario"));
        n.setTipo(rs.getString("tipo"));
        n.setTitulo(rs.getString("titulo"));
        n.setMensaje(rs.getString("mensaje"));
        n.setIcono(rs.getString("icono"));
        n.setColor(rs.getString("color"));
        n.setLeida(rs.getBoolean("leida"));
        n.setReferenciaId(rs.getInt("referencia_id"));
        n.setFechaCreacion(rs.getString("fecha_creacion"));
        return n;
    }
}
