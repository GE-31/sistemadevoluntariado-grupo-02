package com.sistemadevoluntariado.repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.ConexionBD;
import com.sistemadevoluntariado.entity.Permiso;

/**
 * DAO para gestionar permisos directos de un usuario (tabla usuario_permiso).
 * Todas las operaciones se realizan mediante stored procedures:
 *
 *   sp_obtener_todos_permisos()
 *   sp_obtener_permisos_usuario(p_id_usuario)
 *   sp_tiene_permiso(p_id_usuario, p_nombre_permiso, OUT p_resultado)
 *   sp_guardar_permisos_usuario(p_id_usuario, p_ids_permisos)  -- CSV de IDs
 *   sp_eliminar_permisos_usuario(p_id_usuario)
 */
public class PermisoRepository {

    private static final Logger logger = Logger.getLogger(PermisoRepository.class.getName());

    // ── OBTENER TODOS LOS PERMISOS ─────────────────────────
    /**
     * Retorna todos los permisos disponibles en el sistema.
     * SP: sp_obtener_todos_permisos()
     */
    public List<Permiso> obtenerTodosPermisos() {
        List<Permiso> permisos = new ArrayList<>();

        try (Connection con = ConexionBD.getInstance().getConnection();
             CallableStatement cs = con.prepareCall("{CALL sp_obtener_todos_permisos()}");
             ResultSet rs = cs.executeQuery()) {

            while (rs.next()) {
                Permiso p = new Permiso();
                p.setIdPermiso(rs.getInt("id_permiso"));
                p.setNombrePermiso(rs.getString("nombre_permiso"));
                p.setDescripcion(rs.getString("descripcion"));
                permisos.add(p);
            }
            logger.info("✓ Se obtuvieron " + permisos.size() + " permisos");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error en sp_obtener_todos_permisos", e);
        }
        return permisos;
    }

    // ── OBTENER PERMISOS DE UN USUARIO ─────────────────────
    /**
     * Retorna los IDs de los permisos asignados a un usuario.
     * SP: sp_obtener_permisos_usuario(p_id_usuario)
     */
    public List<Integer> obtenerPermisosDeUsuario(int idUsuario) {
        List<Integer> ids = new ArrayList<>();

        try (Connection con = ConexionBD.getInstance().getConnection();
             CallableStatement cs = con.prepareCall("{CALL sp_obtener_permisos_usuario(?)}")) {

            cs.setInt(1, idUsuario);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id_permiso"));
                }
            }
            logger.info("✓ Usuario " + idUsuario + " tiene " + ids.size() + " permisos");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error en sp_obtener_permisos_usuario para usuario " + idUsuario, e);
        }
        return ids;
    }

    // ── VERIFICAR SI UN USUARIO TIENE UN PERMISO ───────────
    /**
     * Verifica si el usuario tiene el permiso indicado por nombre (ej. "personas.ver").
     * SP: sp_tiene_permiso(p_id_usuario, p_nombre_permiso, OUT p_resultado)
     */
    public boolean tienePermiso(int idUsuario, String nombrePermiso) {

        try (Connection con = ConexionBD.getInstance().getConnection();
             CallableStatement cs = con.prepareCall("{CALL sp_tiene_permiso(?, ?, ?)}")) {

            cs.setInt(1, idUsuario);
            cs.setString(2, nombrePermiso);
            cs.registerOutParameter(3, Types.TINYINT);
            cs.execute();

            int resultado = cs.getInt(3);
            return resultado > 0;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error en sp_tiene_permiso para '" + nombrePermiso + "', usuario " + idUsuario, e);
        }
        return false;
    }

    // ── GUARDAR PERMISOS DEL USUARIO ───────────────────────
    /**
     * Reemplaza todos los permisos del usuario con la nueva lista.
     * Convierte la lista de IDs a CSV y lo pasa al SP.
     * SP: sp_guardar_permisos_usuario(p_id_usuario, p_ids_permisos)
     */
    public boolean guardarPermisosUsuario(int idUsuario, List<Integer> idsPermisos) {

        // Convertir lista a CSV: "1,3,5,9"
        String csv = (idsPermisos == null || idsPermisos.isEmpty())
                ? ""
                : idsPermisos.stream()
                             .map(String::valueOf)
                             .reduce((a, b) -> a + "," + b)
                             .orElse("");

        try (Connection con = ConexionBD.getInstance().getConnection();
             CallableStatement cs = con.prepareCall("{CALL sp_guardar_permisos_usuario(?, ?)}")) {

            cs.setInt(1, idUsuario);
            cs.setString(2, csv);
            cs.execute();

            logger.info("✓ Permisos guardados para usuario ID " + idUsuario + ": [" + csv + "]");
            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error en sp_guardar_permisos_usuario para usuario " + idUsuario, e);
        }
        return false;
    }

    // ── ELIMINAR PERMISOS DEL USUARIO ──────────────────────
    /**
     * Elimina todos los permisos directos de un usuario (ej. al eliminarlo).
     * SP: sp_eliminar_permisos_usuario(p_id_usuario)
     */
    public boolean eliminarPermisosUsuario(int idUsuario) {

        try (Connection con = ConexionBD.getInstance().getConnection();
             CallableStatement cs = con.prepareCall("{CALL sp_eliminar_permisos_usuario(?)}")) {

            cs.setInt(1, idUsuario);
            cs.execute();

            logger.info("✓ Permisos eliminados para usuario ID " + idUsuario);
            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error en sp_eliminar_permisos_usuario para usuario " + idUsuario, e);
        }
        return false;
    }
}
