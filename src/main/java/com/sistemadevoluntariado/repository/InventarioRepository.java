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
import jakarta.persistence.TypedQuery;

public class InventarioRepository {
    private static final Logger logger = Logger.getLogger(InventarioRepository.class.getName());
    private EntityManagerFactory emf() {
        return PersistenceManager.getEntityManagerFactory();
    }

    public List<InventarioItem> listar() {
        EntityManager em = emf().createEntityManager();
        try {
            return em.createQuery("SELECT i FROM InventarioItem i ORDER BY i.idItem DESC", InventarioItem.class).getResultList();
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
            StringBuilder jpql = new StringBuilder("SELECT i FROM InventarioItem i WHERE 1=1");
            if (q != null && !q.isEmpty()) jpql.append(" AND (LOWER(i.nombre) LIKE :q OR LOWER(i.categoria) LIKE :q)");
            if (categoria != null && !categoria.isEmpty()) jpql.append(" AND i.categoria = :categoria");
            if (estado != null && !estado.isEmpty()) jpql.append(" AND i.estado = :estado");
            if (stockBajo) jpql.append(" AND i.stockActual <= i.stockMinimo");
            jpql.append(" ORDER BY i.idItem DESC");
            TypedQuery<InventarioItem> query = em.createQuery(jpql.toString(), InventarioItem.class);
            if (q != null && !q.isEmpty()) query.setParameter("q", "%" + q.toLowerCase() + "%");
            if (categoria != null && !categoria.isEmpty()) query.setParameter("categoria", categoria);
            if (estado != null && !estado.isEmpty()) query.setParameter("estado", estado);
            return query.getResultList();
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
            return em.find(InventarioItem.class, id);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener item inventario", e);
            return null;
        } finally {
            em.close();
        }
    }

    public int registrar(InventarioItem item) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(item);
            tx.commit();
            return item.getIdItem();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al registrar item inventario", e);
            throw new RuntimeException("Error al registrar item: " + e.getMessage(), e);
        } finally {
            em.close();
        }
    }

    public boolean actualizar(InventarioItem item) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(item);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al actualizar item inventario", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean registrarMovimiento(int idItem, String tipo, String motivo, double cantidad, String observacion, int idUsuario) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            String tipoUpper = tipo != null ? tipo.trim().toUpperCase() : "";
            if (!tipoUpper.equals("ENTRADA") && !tipoUpper.equals("SALIDA")) {
                throw new IllegalArgumentException("Tipo de movimiento inv\u00e1lido: " + tipo);
            }
            // Obtener stock actual con bloqueo pesimista
            Object stockObj = em.createNativeQuery(
                "SELECT stock_actual FROM inventario_item WHERE id_item = ?1 FOR UPDATE")
                .setParameter(1, idItem)
                .getSingleResult();
            double stockAnterior = stockObj != null ? ((Number)stockObj).doubleValue() : 0.0;
            double stockNuevo;
            if (tipoUpper.equals("ENTRADA")) {
                stockNuevo = stockAnterior + cantidad;
            } else {
                if (stockAnterior < cantidad) throw new IllegalStateException("Stock insuficiente");
                stockNuevo = stockAnterior - cantidad;
            }
            // Actualizar stock
            em.createNativeQuery(
                "UPDATE inventario_item SET stock_actual = ?1, actualizado_en = NOW() WHERE id_item = ?2")
                .setParameter(1, stockNuevo)
                .setParameter(2, idItem)
                .executeUpdate();
            // Registrar movimiento
            em.createNativeQuery(
                "INSERT INTO inventario_movimiento " +
                "(id_item, tipo_movimiento, motivo, cantidad, stock_anterior, stock_nuevo, observacion, id_usuario, creado_en) " +
                "VALUES (?1, ?2, UPPER(TRIM(?3)), ?4, ?5, ?6, ?7, ?8, NOW())")
                .setParameter(1, idItem)
                .setParameter(2, tipoUpper)
                .setParameter(3, motivo != null ? motivo : "MANUAL")
                .setParameter(4, cantidad)
                .setParameter(5, stockAnterior)
                .setParameter(6, stockNuevo)
                .setParameter(7, observacion)
                .setParameter(8, idUsuario)
                .executeUpdate();
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al registrar movimiento inventario", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean cambiarEstado(int idItem, String estado) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("UPDATE InventarioItem i SET i.estado = :estado WHERE i.idItem = :id")
                .setParameter("estado", estado != null ? estado : "INACTIVO")
                .setParameter("id", idItem)
                .executeUpdate();
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al cambiar estado inventario", e);
            return false;
        } finally {
            em.close();
        }
    }

    public int contarStockBajo() {
        EntityManager em = emf().createEntityManager();
        try {
            Long count = em.createQuery(
                "SELECT COUNT(i) FROM InventarioItem i WHERE i.stockActual <= i.stockMinimo AND i.estado = 'ACTIVO'",
                Long.class).getSingleResult();
            return count.intValue();
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
}
