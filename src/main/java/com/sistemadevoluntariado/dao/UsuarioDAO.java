package com.sistemadevoluntariado.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mindrot.jbcrypt.BCrypt;

import com.sistemadevoluntariado.config.ConexionBD;
import com.sistemadevoluntariado.model.Usuario;

public class UsuarioDAO {

    private static final Logger logger = Logger.getLogger(UsuarioDAO.class.getName());

    // Máximo de intentos fallidos permitidos
    private static final int MAX_INTENTOS_FALLIDOS = 3;

    // Tiempo de bloqueo en minutos
    private static final int TIEMPO_BLOQUEO_MINUTOS = 15;

    /**
     * Verifica si una cuenta está bloqueada por intentos fallidos.
     * Si el bloqueo ya expiró, resetea automáticamente los intentos.
     * Usa procedimiento almacenado: sp_verificar_bloqueo
     * 
     * @return minutos restantes de bloqueo, o 0 si no está bloqueada.
     */
    public int verificarBloqueo(String username) {
        String sp = "{CALL sp_verificar_bloqueo(?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) return 0;

            try (CallableStatement cs = conn.prepareCall(sp)) {
                cs.setString(1, username);
                ResultSet rs = cs.executeQuery();

                if (rs.next()) {
                    Timestamp bloqueadoHasta = rs.getTimestamp("bloqueado_hasta");

                    if (bloqueadoHasta != null) {
                        LocalDateTime bloqueoFin = bloqueadoHasta.toLocalDateTime();
                        LocalDateTime ahora = LocalDateTime.now();

                        if (ahora.isBefore(bloqueoFin)) {
                            // Aún está bloqueado
                            long minutosRestantes = ChronoUnit.MINUTES.between(ahora, bloqueoFin) + 1;
                            logger.info("⛔ Cuenta bloqueada para " + username + ", faltan " + minutosRestantes + " min");
                            return (int) minutosRestantes;
                        } else {
                            // El bloqueo expiró, resetear intentos
                            resetearIntentosFallidos(username);
                            return 0;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al verificar bloqueo", e);
        }

        return 0;
    }

    /**
     * Registra un intento fallido de login.
     * Si se alcanza el máximo (3), bloquea la cuenta por 15 minutos.
     * Usa procedimiento almacenado: sp_registrar_intento_fallido
     * 
     * @return número de intentos fallidos actuales, o -1 si se bloqueó la cuenta.
     */
    public int registrarIntentoFallido(String username) {
        String sp = "{CALL sp_registrar_intento_fallido(?, ?, ?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) return 0;

            try (CallableStatement cs = conn.prepareCall(sp)) {
                cs.setString(1, username);
                cs.setInt(2, MAX_INTENTOS_FALLIDOS);
                cs.setInt(3, TIEMPO_BLOQUEO_MINUTOS);
                ResultSet rs = cs.executeQuery();

                if (rs.next()) {
                    int intentos = rs.getInt("intentos_fallidos");

                    if (intentos >= MAX_INTENTOS_FALLIDOS) {
                        logger.warning("⛔ Cuenta bloqueada por " + TIEMPO_BLOQUEO_MINUTOS + " minutos: " + username);
                        return -1; // Indica que se bloqueó
                    }

                    logger.info("⚠ Intento fallido #" + intentos + " de " + MAX_INTENTOS_FALLIDOS + " para: " + username);
                    return intentos;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al registrar intento fallido", e);
        }

        return 0;
    }

    /**
     * Resetea los intentos fallidos y el bloqueo al hacer login exitoso
     * o cuando el tiempo de bloqueo ha expirado.
     * Usa procedimiento almacenado: sp_resetear_intentos_fallidos
     */
    public void resetearIntentosFallidos(String username) {
        String sp = "{CALL sp_resetear_intentos_fallidos(?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) return;

            try (CallableStatement cs = conn.prepareCall(sp)) {
                cs.setString(1, username);
                cs.executeUpdate();
                logger.info("✓ Intentos fallidos reseteados para: " + username);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al resetear intentos fallidos", e);
        }
    }

    /**
     * Obtiene la cantidad de intentos fallidos restantes para un usuario.
     * Usa procedimiento almacenado: sp_obtener_intentos_restantes
     * 
     * @return intentos restantes antes del bloqueo.
     */
    public int obtenerIntentosRestantes(String username) {
        String sp = "{CALL sp_obtener_intentos_restantes(?, ?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) return MAX_INTENTOS_FALLIDOS;

            try (CallableStatement cs = conn.prepareCall(sp)) {
                cs.setString(1, username);
                cs.setInt(2, MAX_INTENTOS_FALLIDOS);
                ResultSet rs = cs.executeQuery();

                if (rs.next()) {
                    return rs.getInt("intentos_restantes");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error al obtener intentos restantes", e);
        }

        return MAX_INTENTOS_FALLIDOS;
    }

    /**
     * Valida las credenciales del usuario usando BCrypt.
     * Usa procedimiento almacenado: sp_obtener_usuario_por_username
     */
    public Usuario validarLogin(String username, String password) {

        String sp = "{CALL sp_obtener_usuario_por_username(?)}";

        try {
            ConexionBD conexion = ConexionBD.getInstance();
            Connection conn = conexion.getConnection();

            if (conn == null) {
                logger.severe("✗ No se pudo establecer conexión a la base de datos");
                return null;
            }

            logger.info(() -> "✓ Conectado a BD - Validando usuario: " + username);

            try (CallableStatement cs = conn.prepareCall(sp)) {
                cs.setString(1, username);

                ResultSet rs = cs.executeQuery();

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

                                // Login exitoso → resetear intentos fallidos
                                resetearIntentosFallidos(username);

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
