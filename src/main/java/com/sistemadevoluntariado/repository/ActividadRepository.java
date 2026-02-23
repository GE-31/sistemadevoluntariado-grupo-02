package com.sistemadevoluntariado.repository;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.PersistenceManager;
import com.sistemadevoluntariado.entity.Actividad;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TypedQuery;

public class ActividadRepository {

    private static final Logger logger = Logger.getLogger(ActividadRepository.class.getName());

    private EntityManagerFactory emf() {
        return PersistenceManager.getEntityManagerFactory();
    }

    // ── CREAR ──────────────────────────────────────────────
    public boolean crearActividad(Actividad actividad) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_crear_actividad");
            spq.registerStoredProcedureParameter("p_nombre",       String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_descripcion",  String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_fecha_inicio", Date.class,    ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_fecha_fin",    Date.class,    ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_ubicacion",    String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_cupo_maximo",  Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_id_usuario",   Integer.class, ParameterMode.IN);
            spq.setParameter("p_nombre",       actividad.getNombre());
            spq.setParameter("p_descripcion",  actividad.getDescripcion());
            spq.setParameter("p_fecha_inicio", actividad.getFechaInicio() != null ? Date.valueOf(actividad.getFechaInicio()) : null);
            spq.setParameter("p_fecha_fin",    actividad.getFechaFin()    != null ? Date.valueOf(actividad.getFechaFin())    : null);
            spq.setParameter("p_ubicacion",    actividad.getUbicacion());
            spq.setParameter("p_cupo_maximo",  actividad.getCupoMaximo());
            spq.setParameter("p_id_usuario",   actividad.getIdUsuario());
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Actividad creada correctamente: " + actividad.getNombre());
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al crear actividad: " + e.getMessage(), e);
            return false;
        } finally {
            em.close();
        }
    }

    // ── LISTAR TODAS ───────────────────────────────────────
    @SuppressWarnings("unchecked")
    public List<Actividad> obtenerTodasActividades() {
        EntityManager em = emf().createEntityManager();
        List<Actividad> actividades = new ArrayList<>();
        try {
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_obtener_todas_actividades", Actividad.class);
            spq.execute();
            actividades = spq.getResultList();
            logger.info("✓ Se obtuvieron " + actividades.size() + " actividades");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener actividades", e);
        } finally {
            em.close();
        }
        return actividades;
    }

    // ── LISTAR SOLO ACTIVAS ─────────────────────────────────
    public List<Actividad> obtenerActividadesActivas() {
        EntityManager em = emf().createEntityManager();
        try {
            TypedQuery<Actividad> query = em.createQuery(
                "SELECT a FROM Actividad a WHERE a.estado = 'ACTIVO' ORDER BY a.idActividad DESC", Actividad.class);
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener actividades activas", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    // ── OBTENER POR ID ─────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Actividad obtenerActividadPorId(int idActividad) {
        EntityManager em = emf().createEntityManager();
        try {
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_obtener_actividad_por_id", Actividad.class);
            spq.registerStoredProcedureParameter("p_id", Integer.class, ParameterMode.IN);
            spq.setParameter("p_id", idActividad);
            spq.execute();
            List<?> result = spq.getResultList();
            if (!result.isEmpty()) {
                logger.info("✓ Actividad obtenida con ID: " + idActividad);
                return (Actividad) result.get(0);
            }
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener actividad", e);
            return null;
        } finally {
            em.close();
        }
    }
 
    // ── ACTUALIZAR ─────────────────────────────────────────
    public boolean actualizarActividad(Actividad actividad) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_actualizar_actividad");
            spq.registerStoredProcedureParameter("p_id",           Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_nombre",       String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_descripcion",  String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_fecha_inicio", Date.class,    ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_fecha_fin",    Date.class,    ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_ubicacion",    String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_cupo_maximo",  Integer.class, ParameterMode.IN);
            spq.setParameter("p_id",           actividad.getIdActividad());
            spq.setParameter("p_nombre",       actividad.getNombre());
            spq.setParameter("p_descripcion",  actividad.getDescripcion());
            spq.setParameter("p_fecha_inicio", actividad.getFechaInicio() != null ? Date.valueOf(actividad.getFechaInicio()) : null);
            spq.setParameter("p_fecha_fin",    actividad.getFechaFin()    != null ? Date.valueOf(actividad.getFechaFin())    : null);
            spq.setParameter("p_ubicacion",    actividad.getUbicacion());
            spq.setParameter("p_cupo_maximo",  actividad.getCupoMaximo());
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Actividad actualizada correctamente: " + actividad.getNombre());
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al actualizar actividad", e);
            return false;
        } finally {
            em.close();
        }
    }

    // ── CAMBIAR ESTADO ─────────────────────────────────────
    public boolean cambiarEstado(int idActividad, String nuevoEstado) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_cambiar_estado_actividad");
            spq.registerStoredProcedureParameter("p_id",     Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_estado", String.class,  ParameterMode.IN);
            spq.setParameter("p_id",     idActividad);
            spq.setParameter("p_estado", nuevoEstado);
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Estado de actividad actualizado a: " + nuevoEstado);
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al cambiar estado de actividad", e);
            return false;
        } finally {
            em.close();
        }
    }

    // ── ELIMINAR ───────────────────────────────────────────
    public boolean eliminarActividad(int idActividad) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_eliminar_actividad");
            spq.registerStoredProcedureParameter("p_id", Integer.class, ParameterMode.IN);
            spq.setParameter("p_id", idActividad);
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Actividad eliminada correctamente");
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al eliminar actividad", e);
            return false;
        } finally {
            em.close();
        }
    }

    public void cerrar() {
        // EMF gestionado por Spring – no cerrar manualmente
    }
}
