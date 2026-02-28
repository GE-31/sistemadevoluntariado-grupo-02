package com.sistemadevoluntariado.repository;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.PersistenceManager;
import com.sistemadevoluntariado.entity.CategoriaInventario;
import com.sistemadevoluntariado.entity.InventarioItem;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

public class InventarioRepository {
    private static final Logger logger = Logger.getLogger(InventarioRepository.class.getName());
    private EntityManagerFactory emf() {
        return PersistenceManager.getEntityManagerFactory();
    }

    public List<InventarioItem> listar() {
        EntityManager em = emf().createEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery("{CALL sp_listar_inventario()}").getResultList();
            return rows.stream().map(this::mapInventarioRow).toList();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al listar inventario", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    public List<InventarioItem> filtrar(String q, String categoria, String estado, boolean stockBajo) {
        EntityManager em = emf().createEntityManager();
        try {
            Query query = em.createNativeQuery("{CALL sp_filtrar_inventario(?,?,?,?)}");
            query.setParameter(1, q);
            query.setParameter(2, categoria);
            query.setParameter(3, estado);
            query.setParameter(4, stockBajo ? 1 : 0);
            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();
            return rows.stream().map(this::mapInventarioRow).toList();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al filtrar inventario", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    public InventarioItem obtenerPorId(int id) {
        EntityManager em = emf().createEntityManager();
        try {
            Query query = em.createNativeQuery("{CALL sp_obtener_item_inventario(?)}");
            query.setParameter(1, id);
            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();
            return rows.isEmpty() ? null : mapInventarioRow(rows.get(0));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener item inventario", e);
            return null;
        } finally {
            em.close();
        }
    }

    public int registrar(InventarioItem item) {
        EntityManager em = emf().createEntityManager();
        try {
            Query query = em.createNativeQuery("{CALL sp_crear_item_inventario(?,?,?,?,?)}");
            query.setParameter(1, item.getNombre());
            query.setParameter(2, item.getCategoria());
            query.setParameter(3, item.getUnidadMedida());
            query.setParameter(4, item.getStockMinimo());
            query.setParameter(5, item.getObservacion());
            Object result = query.getSingleResult();
            return result != null ? ((Number) result).intValue() : 0;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al registrar item inventario", e);
            throw new RuntimeException("Error al registrar item: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public boolean actualizar(InventarioItem item) {
        EntityManager em = emf().createEntityManager();
        try {
            Query query = em.createNativeQuery("{CALL sp_actualizar_item_inventario(?,?,?,?,?,?)}");
            query.setParameter(1, item.getIdItem());
            query.setParameter(2, item.getNombre());
            query.setParameter(3, item.getCategoria());
            query.setParameter(4, item.getUnidadMedida());
            query.setParameter(5, item.getStockMinimo());
            query.setParameter(6, item.getObservacion());
            query.getResultList();
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al actualizar item inventario", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean registrarMovimiento(int idItem, String tipo, String motivo, double cantidad, String observacion, int idUsuario) {
        EntityManager em = emf().createEntityManager();
        try {
            Query query = em.createNativeQuery("{CALL sp_registrar_movimiento_inventario(?,?,?,?,?,?)}");
            query.setParameter(1, idItem);
            query.setParameter(2, tipo);
            query.setParameter(3, motivo);
            query.setParameter(4, cantidad);
            query.setParameter(5, observacion);
            query.setParameter(6, idUsuario);
            query.getResultList();
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al registrar movimiento inventario", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean cambiarEstado(int idItem, String estado) {
        EntityManager em = emf().createEntityManager();
        try {
            Query query = em.createNativeQuery("{CALL sp_cambiar_estado_inventario(?,?)}");
            query.setParameter(1, idItem);
            query.setParameter(2, estado != null ? estado : "INACTIVO");
            query.getResultList();
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al cambiar estado inventario", e);
            return false;
        } finally {
            em.close();
        }
    }

    public int contarStockBajo() {
        EntityManager em = emf().createEntityManager();
        try {
            Object result = em.createNativeQuery("{CALL sp_contar_stock_bajo()}").getSingleResult();
            return result != null ? ((Number) result).intValue() : 0;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al contar stock bajo", e);
            return 0;
        } finally {
            em.close();
        }
    }

    // ── CATEGORÍAS ──────────────────────────────────────────────────────────

    public List<CategoriaInventario> listarCategorias() {
        EntityManager em = emf().createEntityManager();
        try {
            return em.createQuery("SELECT c FROM CategoriaInventario c ORDER BY c.idCategoria ASC", CategoriaInventario.class)
                     .getResultList();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al listar categorias", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    public boolean registrarCategoria(CategoriaInventario cat) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(cat);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al registrar categoria", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean actualizarCategoria(CategoriaInventario cat) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(cat);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al actualizar categoria", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean eliminarCategoria(int idCategoria) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            CategoriaInventario cat = em.find(CategoriaInventario.class, idCategoria);
            if (cat != null) em.remove(cat);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al eliminar categoria", e);
            return false;
        } finally {
            em.close();
        }
    }

    public CategoriaInventario obtenerCategoriaPorId(int id) {
        EntityManager em = emf().createEntityManager();
        try {
            return em.find(CategoriaInventario.class, id);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener categoria", e);
            return null;
        } finally {
            em.close();
        }
    }

    private InventarioItem mapInventarioRow(Object[] row) {
        InventarioItem item = new InventarioItem();
        item.setIdItem(row[0] != null ? ((Number) row[0]).intValue() : 0);
        item.setNombre(row[1] != null ? String.valueOf(row[1]) : null);
        item.setCategoria(row[2] != null ? String.valueOf(row[2]) : null);
        item.setUnidadMedida(row[3] != null ? String.valueOf(row[3]) : null);
        item.setStockActual(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0);
        item.setStockMinimo(row[5] != null ? ((Number) row[5]).doubleValue() : 0.0);
        item.setEstado(row[6] != null ? String.valueOf(row[6]) : null);
        item.setObservacion(row[7] != null ? String.valueOf(row[7]) : null);
        item.setCreadoEn(row[8] != null ? String.valueOf(row[8]) : null);
        item.setActualizadoEn(row[9] != null ? String.valueOf(row[9]) : null);
        return item;
    }
}
