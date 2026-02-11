package com.sistemadevoluntariado.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mindrot.jbcrypt.BCrypt;

import com.sistemadevoluntariado.config.ConexionBD;
import com.sistemadevoluntariado.model.Usuario;

public class UsuarioDAO {

    private static final Logger logger = Logger.getLogger(UsuarioDAO.class.getName());

    /**
     * Valida las credenciales del usuario usando BCrypt.
     */
    public Usuario validarLogin(String username, String password) {

        String sql = "SELECT id_usuario, nombres, apellidos, correo, username, dni, password_hash, foto_perfil, estado, creado_en, actualizado_en FROM usuario WHERE username = ?";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return null;
            }

            logger.info(() -> "✓ Conectado a BD - Validando usuario: " + username);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String hashBD = rs.getString("password_hash");

                    // Validar que la contraseña no sea nula ni vacía y sea un hash BCrypt válido
                    if (hashBD != null && !hashBD.isEmpty() && isBCryptHash(hashBD)) {
                        try {
                            if (BCrypt.checkpw(password, hashBD)) {
                                Usuario u = new Usuario();
                                u.setIdUsuario(rs.getInt("id_usuario"));
                                u.setNombres(rs.getString("nombres"));
                                u.setApellidos(rs.getString("apellidos"));
                                u.setCorreo(rs.getString("correo"));
                                u.setUsername(rs.getString("username"));
                                u.setDni(rs.getString("dni"));
                                u.setFotoPerfil(rs.getString("foto_perfil"));
                                u.setEstado(rs.getString("estado"));
                                u.setCreadoEn(rs.getString("creado_en"));
                                u.setActualizadoEn(rs.getString("actualizado_en"));

                                logger.info(() -> "✓ Login exitoso para usuario: " + username);
                                return u;
                            } else {
                                logger.warning(() -> "✗ Contraseña incorrecta para usuario: " + username);
                            }
                        } catch (IllegalArgumentException e) {
                            logger.warning(() -> "✗ Hash inválido para usuario: " + username);
                        }
                    } else {
                        logger.warning(() -> "✗ Hash BCrypt inválido o no encontrado para usuario: " + username);
                    }
                } else {
                    logger.info(() -> "✗ Usuario no encontrado en BD: " + username);
                }
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error en validarLogin", e);
        }

        return null;
    }

    /**
     * Verifica si una cadena es un hash BCrypt válido.
     */
    private boolean isBCryptHash(String hash) {
        // Un hash BCrypt válido comienza con $2a$, $2b$, $2x$ o $2y$
        return hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") ||
                hash.startsWith("$2x$") || hash.startsWith("$2y$"));
    }

    /**
     * Registrar usuario (con contraseña encriptada).
     */
    public boolean registrarUsuario(String nombres, String apellidos, String correo, String username, String password,
            String dni) {

        String storedProcedure = "{CALL sp_crear_usuario(?, ?, ?, ?, ?, ?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            try (CallableStatement cs = conn.prepareCall(storedProcedure)) {
                String hash = BCrypt.hashpw(password, BCrypt.gensalt());

                logger.info("► Parámetros enviados al SP: nombres=" + nombres + ", apellidos=" + apellidos + ", correo="
                        + correo + ", username=" + username + ", dni=" + dni);

                cs.setString(1, nombres);
                cs.setString(2, apellidos);
                cs.setString(3, correo);
                cs.setString(4, username);
                cs.setString(5, dni);
                cs.setString(6, hash);

                cs.execute();

                logger.info(() -> "✓ Usuario registrado exitosamente: " + username);
                return true;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al registrar usuario: " + e.getMessage(), e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Registrar usuario basado en un voluntario existente y rol del sistema
     */
    public boolean registrarUsuarioConVoluntario(int voluntarioId, int rolSistemaId, String username, String password) {
        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            // Primero obtener los datos del voluntario
            String sqlVoluntario = "SELECT nombres, apellidos, dni, correo FROM voluntario WHERE id_voluntario = ?";
            String nombres = null, apellidos = null, dni = null, correo = null;

            try (PreparedStatement ps = conn.prepareStatement(sqlVoluntario)) {
                ps.setInt(1, voluntarioId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        nombres = rs.getString("nombres");
                        apellidos = rs.getString("apellidos");
                        dni = rs.getString("dni");
                        correo = rs.getString("correo");
                    } else {
                        logger.severe("✗ No se encontró el voluntario con ID: " + voluntarioId);
                        return false;
                    }
                }
            }

            // Encriptar la contraseña
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());

            // Crear el usuario con los datos del voluntario
            String sqlUsuario = "INSERT INTO usuario (nombres, apellidos, correo, username, dni, password_hash, estado, creado_en, actualizado_en) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, 'Activo', NOW(), NOW())";

            try (PreparedStatement ps = conn.prepareStatement(sqlUsuario, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombres);
                ps.setString(2, apellidos);
                ps.setString(3, correo);
                ps.setString(4, username);
                ps.setString(5, dni);
                ps.setString(6, hash);

                int rowsAffected = ps.executeUpdate();

                if (rowsAffected > 0) {
                    // Obtener el ID del usuario creado
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            int idUsuario = rs.getInt(1);

                            // Actualizar el voluntario para vincular con el usuario
                            String updateVoluntario = "UPDATE voluntario SET id_usuario = ? WHERE id_voluntario = ?";
                            try (PreparedStatement psUpdate = conn.prepareStatement(updateVoluntario)) {
                                psUpdate.setInt(1, idUsuario);
                                psUpdate.setInt(2, voluntarioId);
                                psUpdate.executeUpdate();
                            }

                            logger.info("✓ Usuario creado exitosamente desde voluntario: " + username + " (ID Usuario: "
                                    + idUsuario + ")");
                            return true;
                        }
                    }
                }
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al registrar usuario con voluntario: " + e.getMessage(), e);
        }

        return false;
    }

    /**
     * Obtener todos los usuarios
     */
    public java.util.List<Usuario> obtenerTodosUsuarios() {
        java.util.List<Usuario> usuarios = new java.util.ArrayList<>();
        String sql = "SELECT id_usuario, nombres, apellidos, correo, username, dni, foto_perfil, estado, creado_en, actualizado_en FROM usuario ORDER BY id_usuario DESC";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return usuarios;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Usuario u = new Usuario();
                    u.setIdUsuario(rs.getInt("id_usuario"));
                    u.setNombres(rs.getString("nombres"));
                    u.setApellidos(rs.getString("apellidos"));
                    u.setCorreo(rs.getString("correo"));
                    u.setUsername(rs.getString("username"));
                    u.setDni(rs.getString("dni"));
                    u.setFotoPerfil(rs.getString("foto_perfil"));
                    u.setEstado(rs.getString("estado"));
                    u.setCreadoEn(rs.getString("creado_en"));
                    u.setActualizadoEn(rs.getString("actualizado_en"));

                    usuarios.add(u);
                }
                logger.info("✓ Se obtuvieron " + usuarios.size() + " usuarios");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener usuarios", e);
        }

        return usuarios;
    }

    /**
     * Obtener usuario por ID
     */
    public Usuario obtenerUsuarioPorId(int idUsuario) {
        String sql = "SELECT id_usuario, nombres, apellidos, correo, username, dni, foto_perfil, estado, creado_en, actualizado_en FROM usuario WHERE id_usuario = ?";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return null;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idUsuario);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Usuario u = new Usuario();
                        u.setIdUsuario(rs.getInt("id_usuario"));
                        u.setNombres(rs.getString("nombres"));
                        u.setApellidos(rs.getString("apellidos"));
                        u.setCorreo(rs.getString("correo"));
                        u.setUsername(rs.getString("username"));
                        u.setDni(rs.getString("dni"));
                        u.setFotoPerfil(rs.getString("foto_perfil"));
                        u.setEstado(rs.getString("estado"));
                        u.setCreadoEn(rs.getString("creado_en"));
                        u.setActualizadoEn(rs.getString("actualizado_en"));

                        logger.info("✓ Usuario obtenido: " + u.getNombres());
                        return u;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener usuario", e);
        }

        return null;
    }

    /**
     * Actualizar usuario usando procedimiento almacenado
     */
    public boolean actualizarUsuario(int idUsuario, String nombres, String apellidos, String correo, String username,
            String dni) {
        String sql = "{CALL sp_actualizar_usuario(?, ?, ?, ?, ?, ?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            try (java.sql.CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idUsuario);
                cs.setString(2, nombres);
                cs.setString(3, apellidos != null ? apellidos : "");
                cs.setString(4, correo != null ? correo : "");
                cs.setString(5, username);
                cs.setString(6, dni != null ? dni : "");

                cs.execute();
                logger.info("✓ Usuario actualizado correctamente: " + nombres);
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al actualizar usuario", e);
        }

        return false;
    }

    /**
     * Cambiar estado del usuario usando procedimiento almacenado
     */
    public boolean cambiarEstadoUsuario(int idUsuario, String nuevoEstado) {
        String sql = "{CALL sp_cambiar_estado_usuario(?, ?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            try (java.sql.CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idUsuario);
                cs.setString(2, nuevoEstado);

                cs.execute();
                logger.info("✓ Estado del usuario actualizado a: " + nuevoEstado);
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al cambiar estado del usuario", e);
        }

        return false;
    }

    /**
     * Actualizar foto de perfil del usuario
     */
    public boolean actualizarFotoPerfil(int idUsuario, String fotoPerfil) {
        String sql = "{CALL sp_actualizar_foto_perfil(?, ?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            try (CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idUsuario);
                cs.setString(2, fotoPerfil);

                cs.execute();
                logger.info("✓ Foto de perfil actualizada para usuario ID: " + idUsuario);
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al actualizar foto de perfil", e);
        }

        return false;
    }

    /**
     * Eliminar usuario usando procedimiento almacenado
     */
    public boolean eliminarUsuario(int idUsuario) {
        String sql = "{CALL sp_eliminar_usuario(?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return false;
            }

            try (java.sql.CallableStatement cs = conn.prepareCall(sql)) {
                cs.setInt(1, idUsuario);

                cs.execute();
                logger.info("✓ Usuario eliminado correctamente");
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al eliminar usuario", e);
        }

        return false;
    }
}
