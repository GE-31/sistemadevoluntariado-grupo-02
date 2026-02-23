package com.sistemadevoluntariado.repository;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.PersistenceManager;
import com.sistemadevoluntariado.entity.Calendario;
 
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

public class CalendarioRepository {
    private static final Logger logger = Logger.getLogger(CalendarioRepository.class.getName());
    private EntityManagerFactory emf() {
        return PersistenceManager.getEntityManagerFactory();
    }

    public boolean crearEvento(Calendario c) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(c);
            tx.commit();
            logger.info("✓ Evento creado: " + c.getTitulo());
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "✗ Error al crear evento", e);
            return false;
        } finally {
            em.close();
        }
    }

    public List<Calendario> listarEventos() {
        EntityManager em = emf().createEntityManager();
        try {
            TypedQuery<Calendario> query = em.createQuery(
                "SELECT c FROM Calendario c ORDER BY c.fechaInicio ASC", Calendario.class);
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al listar eventos", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    public Calendario obtenerPorId(int idEvento) {
        EntityManager em = emf().createEntityManager();
        try {
            return em.find(Calendario.class, idEvento);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener evento", e);
            return null;
        } finally {
            em.close();
        }
    }

    public boolean actualizarEvento(Calendario c) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(c);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "✗ Error al actualizar evento", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean eliminarEvento(int idEvento) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Calendario c = em.find(Calendario.class, idEvento);
            if (c != null) {
                em.remove(c);
                tx.commit();
                return true;
            }
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "✗ Error al eliminar evento", e);
        } finally {
            em.close();
        }
        return false;
    }
}
