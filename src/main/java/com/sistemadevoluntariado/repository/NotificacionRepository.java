package com.sistemadevoluntariado.repository;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.PersistenceManager;
import com.sistemadevoluntariado.entity.Notificacion;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

public class NotificacionRepository {
    private static final Logger logger = Logger.getLogger(NotificacionRepository.class.getName());
    private EntityManagerFactory emf() {
        return PersistenceManager.getEntityManagerFactory();
    }

    public boolean crearNotificacion(Notificacion n) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(n);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al crear notificacion", e);
            return false;
        } finally {
            em.close();
        }
    }

    public List<Notificacion> listarPorUsuario(int idUsuario) {
        EntityManager em = emf().createEntityManager();
        try {
            TypedQuery<Notificacion> query = em.createQuery(
                "SELECT n FROM Notificacion n WHERE n.idUsuario = :idUsuario ORDER BY n.fechaCreacion DESC",
                Notificacion.class);
            query.setParameter("idUsuario", idUsuario);
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al listar notificaciones", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    public int contarNoLeidas(int idUsuario) {
        EntityManager em = emf().createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(n) FROM Notificacion n WHERE n.idUsuario = :idUsuario AND n.leida = false",
                Long.class)
                .setParameter("idUsuario", idUsuario)
                .getSingleResult();
            return count.intValue();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al contar notificaciones", e);
            return 0;
        } finally {
            em.close();
        }
    }

    public boolean marcarLeida(int idNotificacion) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("UPDATE Notificacion n SET n.leida = true WHERE n.idNotificacion = :id")
                .setParameter("id", idNotificacion)
                .executeUpdate();
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al marcar notificacion como leida", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean marcarTodasLeidas(int idUsuario) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("UPDATE Notificacion n SET n.leida = true WHERE n.idUsuario = :idUsuario")
                .setParameter("idUsuario", idUsuario)
                .executeUpdate();
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al marcar todas como leidas", e);
            return false;
        } finally {
            em.close();
        }
    }

    public void generarNotificacionesActividadesHoy(int idUsuario) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createNativeQuery(
                "INSERT INTO notificaciones (id_usuario, tipo, titulo, mensaje, icono, color, referencia_id) " +
                "SELECT ?1, 'ACTIVIDAD_HOY', " +
                "CONCAT('Actividad hoy: ', a.nombre), " +
                "CONCAT('La actividad \"', a.nombre, '\" est\u00e1 programada para hoy en ', IFNULL(a.ubicacion, 'ubicaci\u00f3n por definir'), '.'), " +
                "'fa-calendar-check', '#10b981', a.id_actividad " +
                "FROM actividades a " +
                "WHERE DATE(a.fecha_inicio) = CURDATE() " +
                "  AND a.estado = 'ACTIVO' " +
                "  AND NOT EXISTS ( " +
                "      SELECT 1 FROM notificaciones n " +
                "      WHERE n.id_usuario = ?1 AND n.tipo = 'ACTIVIDAD_HOY' " +
                "        AND n.referencia_id = a.id_actividad " +
                "        AND DATE(n.fecha_creacion) = CURDATE()"  +
                "  )")
                .setParameter(1, idUsuario)
                .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.WARNING, "Error al generar notificaciones de actividades: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    public void generarNotificacionesEventosHoy(int idUsuario) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createNativeQuery(
                "INSERT INTO notificaciones (id_usuario, titulo, mensaje, tipo, leida, creada_en) " +
                "SELECT ?1, CONCAT('Evento hoy: ', e.titulo), " +
                "CONCAT('Tienes programado \"', e.titulo, '\" para hoy'), 'EVENTO', 0, NOW() " +
                "FROM eventos_calendario e " +
                "WHERE DATE(e.fecha_inicio) = CURDATE() AND e.id_usuario = ?1 " +
                "  AND NOT EXISTS ( " +
                "      SELECT 1 FROM notificaciones n " +
                "      WHERE n.id_usuario = ?1 AND n.tipo = 'EVENTO' " +
                "        AND n.titulo = CONCAT('Evento hoy: ', e.titulo) " +
                "        AND DATE(n.creada_en) = CURDATE() " +
                "  )")
                .setParameter(1, idUsuario)
                .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.WARNING, "Error al generar notificaciones de eventos: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
