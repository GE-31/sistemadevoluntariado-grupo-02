package com.sistemadevoluntariado.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.PersistenceManager;
import com.sistemadevoluntariado.entity.Voluntario;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TypedQuery;

public class VoluntarioRepository {
    private static final Logger logger = Logger.getLogger(VoluntarioRepository.class.getName());
    private EntityManagerFactory emf() {
        return PersistenceManager.getEntityManagerFactory();
    }

    // ── CREAR ──────────────────────────────────────────────
    public boolean crearVoluntario(Voluntario voluntario) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_crear_voluntario");
            spq.registerStoredProcedureParameter("p_nombres",    String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_apellidos",  String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_dni",        String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_correo",     String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_telefono",   String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_carrera",    String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_id_usuario", Integer.class, ParameterMode.IN);
            spq.setParameter("p_nombres",    voluntario.getNombres());
            spq.setParameter("p_apellidos",  voluntario.getApellidos());
            spq.setParameter("p_dni",        voluntario.getDni());
            spq.setParameter("p_correo",     voluntario.getCorreo());
            spq.setParameter("p_telefono",   voluntario.getTelefono());
            spq.setParameter("p_carrera",    voluntario.getCarrera());
            spq.setParameter("p_id_usuario", voluntario.getIdUsuario() != null ? voluntario.getIdUsuario() : 0);
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Voluntario creado: " + voluntario.getNombres());
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al crear voluntario: " + e.getMessage(), e);
        } finally {
            em.close();
        }
        return false;
    }

    // ── OBTENER TODOS ───────────────────────────────────────
    public List<Voluntario> obtenerTodosVoluntarios() {
        EntityManager em = emf().createEntityManager();
        List<Voluntario> lista = new ArrayList<>();
        try {
            TypedQuery<Voluntario> query = em.createQuery(
                "SELECT v FROM Voluntario v ORDER BY v.idVoluntario DESC", Voluntario.class);
            lista = query.getResultList();
            logger.info("✓ Se obtuvieron " + lista.size() + " voluntarios");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener voluntarios", e);
        } finally {
            em.close();
        }
        return lista;
    }

    // ── VOLUNTARIOS QUE TIENEN ASISTENCIA REGISTRADA ──────────
    @SuppressWarnings("unchecked")
    public List<Voluntario> obtenerVoluntariosConAsistencia() {
        EntityManager em = emf().createEntityManager();
        List<Voluntario> lista = new ArrayList<>();
        try {
            List<Object[]> rows = em.createNativeQuery(
                "SELECT DISTINCT v.id_voluntario, v.nombres, v.apellidos, v.dni " +
                "FROM voluntario v INNER JOIN asistencias a ON v.id_voluntario = a.id_voluntario " +
                "WHERE a.estado IN ('ASISTIO','TARDANZA') AND v.estado = 'ACTIVO' " +
                "ORDER BY v.apellidos, v.nombres")
                .getResultList();
            for (Object[] row : rows) {
                Voluntario vol = new Voluntario();
                vol.setIdVoluntario(row[0] != null ? ((Number) row[0]).intValue() : 0);
                vol.setNombres(row[1] != null ? row[1].toString() : "");
                vol.setApellidos(row[2] != null ? row[2].toString() : "");
                vol.setDni(row[3] != null ? row[3].toString() : "");
                vol.setEstado("ACTIVO");
                lista.add(vol);
            }
            logger.info("✓ Voluntarios con asistencia: " + lista.size());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener voluntarios con asistencia", e);
        } finally {
            em.close();
        }
        return lista;
    }

    // ── VOLUNTARIOS CON ACCESO (sin SP — usa JPQL) ────────────
    public List<Voluntario> obtenerVoluntariosConAcceso() {
        EntityManager em = emf().createEntityManager();
        try {
            TypedQuery<Voluntario> query = em.createQuery(
                "SELECT v FROM Voluntario v WHERE v.accesoSistema = true " +
                "AND v.idUsuario IS NULL ORDER BY v.nombres ASC", Voluntario.class);
            List<Voluntario> lista = query.getResultList();
            logger.info("✓ Voluntarios con acceso disponibles: " + lista.size());
            return lista;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener voluntarios con acceso", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    // ── OBTENER POR ID ──────────────────────────────────────
    public Voluntario obtenerVoluntarioPorId(int idVoluntario) {
        EntityManager em = emf().createEntityManager();
        try {
            return em.find(Voluntario.class, idVoluntario);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener voluntario por ID", e);
            return null;
        } finally {
            em.close();
        }
    }

    // ── ACTUALIZAR ──────────────────────────────────────────
    public boolean actualizarVoluntario(Voluntario voluntario) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_actualizar_voluntario");
            spq.registerStoredProcedureParameter("p_id_voluntario", Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_nombres",       String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_apellidos",     String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_dni",           String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_correo",        String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_telefono",      String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_carrera",       String.class,  ParameterMode.IN);
            spq.setParameter("p_id_voluntario", voluntario.getIdVoluntario());
            spq.setParameter("p_nombres",       voluntario.getNombres());
            spq.setParameter("p_apellidos",     voluntario.getApellidos());
            spq.setParameter("p_dni",           voluntario.getDni());
            spq.setParameter("p_correo",        voluntario.getCorreo());
            spq.setParameter("p_telefono",      voluntario.getTelefono());
            spq.setParameter("p_carrera",       voluntario.getCarrera());
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Voluntario actualizado: " + voluntario.getNombres());
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al actualizar voluntario", e);
        } finally {
            em.close();
        }
        return false;
    }

    // ── CAMBIAR ESTADO ─────────────────────────────────────
    public boolean cambiarEstado(int idVoluntario, String nuevoEstado) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_cambiar_estado_voluntario");
            spq.registerStoredProcedureParameter("p_id_voluntario", Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_estado",        String.class,  ParameterMode.IN);
            spq.setParameter("p_id_voluntario", idVoluntario);
            spq.setParameter("p_estado",        nuevoEstado);
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Estado del voluntario actualizado a: " + nuevoEstado);
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al cambiar estado del voluntario", e);
        } finally {
            em.close();
        }
        return false;
    }

    // ── ELIMINAR ────────────────────────────────────────────
    public boolean eliminarVoluntario(int idVoluntario) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_eliminar_voluntario");
            spq.registerStoredProcedureParameter("p_id_voluntario", Integer.class, ParameterMode.IN);
            spq.setParameter("p_id_voluntario", idVoluntario);
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Voluntario eliminado correctamente ID: " + idVoluntario);
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al eliminar voluntario", e);
        } finally {
            em.close();
        }
        return false;
    }
}
