package com.sistemadevoluntariado.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mindrot.jbcrypt.BCrypt;

import com.sistemadevoluntariado.entity.Usuario;
import com.sistemadevoluntariado.util.DatabaseUtil;

public class UsuarioRepository {

    private static final Logger logger = Logger.getLogger(UsuarioRepository.class.getName());

    // -- MAPEO ResultSet -> Usuario
    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombres(rs.getString("nombres"));
        u.setApellidos(rs.getString("apellidos"));
        u.setCorreo(rs.getString("correo"));
        u.setUsername(rs.getString("username"));
        u.setDni(rs.getString("dni"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFotoPerfil(rs.getString("foto_perfil"));
        u.setEstado(rs.getString("estado"));
        u.setCreadoEn(rs.getString("creado_en"));
        u.setActualizadoEn(rs.getString("actualizado_en"));
        return u;
    }

    // -- OBTENER USUARIO POR USERNAME
    public Usuario obtenerUsuarioPorUsername(String username) {
        String sql = "SELECT id_usuario, nombres, apellidos, correo, username, dni, " +
                     "password_hash, foto_perfil, estado, creado_en, actualizado_en " +
                     "FROM usuario WHERE username = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error en obtenerUsuarioPorUsername", e);
        }
        return null;
    }

    // -- OBTENER USUARIO POR ID
    public Usuario obtenerUsuarioPorId(int idUsuario) {
        String sql = "SELECT id_usuario, nombres, apellidos, correo, username, dni, " +
                     "password_hash, foto_perfil, estado, creado_en, actualizado_en " +
                     "FROM usuario WHERE id_usuario = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error en obtenerUsuarioPorId", e);
        }
        return null;
    }

    // -- OBTENER TODOS LOS USUARIOS
    public List<Usuario> obtenerTodosUsuarios() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id_usuario, nombres, apellidos, correo, username, dni, " +
                     "password_hash, foto_perfil, estado, creado_en, actualizado_en " +
                     "FROM usuario ORDER BY id_usuario DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error en obtenerTodosUsuarios", e);
        }
        return lista;
    }

    // -- REGISTRAR USUARIO CON VOLUNTARIO
    public boolean registrarUsuarioConVoluntario(int voluntarioId, int rolSistemaId,
                                                  String username, String password) {
        if (obtenerUsuarioPorUsername(username) != null) {
            logger.warning("Username ya existe: " + username);
            return false;
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String sqlVol = "SELECT nombres, apellidos, correo FROM voluntario WHERE id_voluntario = ?";
        String nombres = null, apellidos = null, correo = null;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlVol)) {
            ps.setInt(1, voluntarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nombres   = rs.getString("nombres");
                    apellidos = rs.getString("apellidos");
                    correo    = rs.getString("correo");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener voluntario", e); return false;
        }
        if (nombres == null) { logger.warning("Voluntario no encontrado: " + voluntarioId); return false; }
        if (correo == null || correo.trim().isEmpty()) correo = username + "@sistema.local";

        int nuevoId = -1;
        String sqlUser = "INSERT INTO usuario (nombres, apellidos, correo, username, password_hash, estado, creado_en, actualizado_en) " +
                         "VALUES (?, ?, ?, ?, ?, 'ACTIVO', NOW(), NOW())";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUser, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombres); ps.setString(2, apellidos); ps.setString(3, correo);
            ps.setString(4, username); ps.setString(5, hash);
            if (ps.executeUpdate() == 0) return false;
            try (ResultSet gen = ps.getGeneratedKeys()) { if (gen.next()) nuevoId = gen.getInt(1); }
        } catch (SQLException e) { logger.log(Level.SEVERE, "Error al insertar usuario", e); return false; }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO usuario_rol (id_usuario, id_rol_sistema, asignado_en) VALUES (?, ?, NOW())")) {
            ps.setInt(1, nuevoId); ps.setInt(2, rolSistemaId); ps.executeUpdate();
        } catch (SQLException e) { logger.log(Level.WARNING, "Error al asignar rol", e); }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE voluntario SET id_usuario = ? WHERE id_voluntario = ?")) {
            ps.setInt(1, nuevoId); ps.setInt(2, voluntarioId); ps.executeUpdate();
        } catch (SQLException e) { logger.log(Level.WARNING, "Error al vincular voluntario", e); }

        logger.info("Usuario creado: " + username);
        return true;
    }

    // -- CAMBIAR ESTADO
    public boolean cambiarEstadoUsuario(int idUsuario, String nuevoEstado) {
        String sql = "UPDATE usuario SET estado = ?, actualizado_en = NOW() WHERE id_usuario = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado); ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { logger.log(Level.SEVERE, "Error en cambiarEstadoUsuario", e); }
        return false;
    }

    // -- ACTUALIZAR FOTO DE PERFIL
    public boolean actualizarFotoPerfil(int idUsuario, String fotoPerfil) {
        String sql = "UPDATE usuario SET foto_perfil = ?, actualizado_en = NOW() WHERE id_usuario = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fotoPerfil); ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { logger.log(Level.SEVERE, "Error en actualizarFotoPerfil", e); }
        return false;
    }

    // -- ELIMINAR USUARIO
    public boolean eliminarUsuario(int idUsuario) {
        String sql = "DELETE FROM usuario WHERE id_usuario = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { logger.log(Level.SEVERE, "Error en eliminarUsuario", e); }
        return false;
    }

    // -- VALIDAR LOGIN (fallback manual)
    public Usuario validarLogin(String username, String password) {
        Usuario u = obtenerUsuarioPorUsername(username);
        if (u != null && u.getPasswordHash() != null && BCrypt.checkpw(password, u.getPasswordHash())) {
            return u;
        }
        return null;
    }
}