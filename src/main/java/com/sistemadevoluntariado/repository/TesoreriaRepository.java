package com.sistemadevoluntariado.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.PersistenceManager;
import com.sistemadevoluntariado.entity.MovimientoFinanciero;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

public class TesoreriaRepository {
    private static final Logger logger = Logger.getLogger(TesoreriaRepository.class.getName());
    private EntityManagerFactory emf() {
        return PersistenceManager.getEntityManagerFactory();
    }

    @SuppressWarnings("unchecked")
    public List<MovimientoFinanciero> listar() {
        EntityManager em = emf().createEntityManager();
        try {
            String sql =
                "SELECT mf.id_movimiento, mf.tipo, mf.categoria, mf.monto, mf.descripcion, " +
                "mf.fecha_movimiento, mf.id_actividad, mf.id_usuario, mf.comprobante, mf.creado_en, " +
                "a.nombre AS nombre_actividad, CONCAT(u.nombres,' ',u.apellidos) AS nombre_usuario " +
                "FROM movimiento_financiero mf " +
                "LEFT JOIN actividades a ON mf.id_actividad = a.id_actividad " +
                "LEFT JOIN usuario u ON mf.id_usuario = u.id_usuario " +
                "ORDER BY mf.id_movimiento DESC";
            List<Object[]> rows = em.createNativeQuery(sql).getResultList();
            List<MovimientoFinanciero> lista = new ArrayList<>();
            for (Object[] row : rows) {
                MovimientoFinanciero mf = new MovimientoFinanciero();
                mf.setIdMovimiento(row[0] != null ? ((Number)row[0]).intValue() : 0);
                mf.setTipo((String)row[1]);
                mf.setCategoria((String)row[2]);
                mf.setMonto(row[3] != null ? ((Number)row[3]).doubleValue() : 0.0);
                mf.setDescripcion((String)row[4]);
                mf.setFechaMovimiento(row[5] != null ? row[5].toString() : null);
                mf.setIdActividad(row[6] != null ? ((Number)row[6]).intValue() : 0);
                mf.setIdUsuario(row[7] != null ? ((Number)row[7]).intValue() : 0);
                mf.setComprobante((String)row[8]);
                mf.setCreadoEn(row[9] != null ? row[9].toString() : null);
                mf.setActividad((String)row[10]);
                mf.setUsuarioRegistro((String)row[11]);
                lista.add(mf);
            }
            logger.info("Se listaron " + lista.size() + " movimientos financieros");
            return lista;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al listar movimientos", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    public boolean registrar(MovimientoFinanciero mf) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(mf);
            tx.commit();
            logger.info("Movimiento financiero registrado correctamente");
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al registrar movimiento: " + e.getMessage(), e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean actualizar(MovimientoFinanciero mf) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(mf);
            tx.commit();
            logger.info("Movimiento financiero actualizado correctamente");
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al actualizar movimiento: " + e.getMessage(), e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean eliminar(int id) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            MovimientoFinanciero mf = em.find(MovimientoFinanciero.class, id);
            if (mf != null) em.remove(mf);
            tx.commit();
            logger.info("Movimiento financiero eliminado correctamente");
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al eliminar movimiento", e);
            return false;
        } finally {
            em.close();
        }
    }

    public MovimientoFinanciero obtenerPorId(int id) {
        EntityManager em = emf().createEntityManager();
        try {
            return em.find(MovimientoFinanciero.class, id);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener movimiento por ID", e);
            return null;
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Double> obtenerBalance() {
        EntityManager em = emf().createEntityManager();
        try {
            String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN tipo='INGRESO' THEN monto ELSE 0 END),0) AS total_ingresos, " +
                "COALESCE(SUM(CASE WHEN tipo='GASTO' THEN monto ELSE 0 END),0) AS total_gastos, " +
                "COALESCE(SUM(CASE WHEN tipo='INGRESO' THEN monto ELSE -monto END),0) AS balance_neto " +
                "FROM movimiento_financiero";
            Object[] row = (Object[]) em.createNativeQuery(sql).getSingleResult();
            Map<String, Double> balance = new HashMap<>();
            balance.put("ingresos", ((Number)row[0]).doubleValue());
            balance.put("gastos",   ((Number)row[1]).doubleValue());
            balance.put("saldo",    ((Number)row[2]).doubleValue());
            return balance;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener balance", e);
            Map<String, Double> vacio = new HashMap<>();
            vacio.put("ingresos", 0.0);
            vacio.put("gastos",   0.0);
            vacio.put("saldo",    0.0);
            return vacio;
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    public List<MovimientoFinanciero> filtrar(String tipo, String categoria, String fechaInicio, String fechaFin) {
        EntityManager em = emf().createEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT m FROM MovimientoFinanciero m WHERE 1=1");
            if (tipo != null && !tipo.isEmpty()) jpql.append(" AND m.tipo = :tipo");
            if (categoria != null && !categoria.isEmpty()) jpql.append(" AND m.categoria = :categoria");
            if (fechaInicio != null && !fechaInicio.isEmpty()) jpql.append(" AND m.fechaMovimiento >= :fi");
            if (fechaFin != null && !fechaFin.isEmpty()) jpql.append(" AND m.fechaMovimiento <= :ff");
            jpql.append(" ORDER BY m.idMovimiento DESC");
            var q = em.createQuery(jpql.toString(), MovimientoFinanciero.class);
            if (tipo != null && !tipo.isEmpty()) q.setParameter("tipo", tipo);
            if (categoria != null && !categoria.isEmpty()) q.setParameter("categoria", categoria);
            if (fechaInicio != null && !fechaInicio.isEmpty()) q.setParameter("fi", fechaInicio);
            if (fechaFin != null && !fechaFin.isEmpty()) q.setParameter("ff", fechaFin);
            return q.getResultList();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al filtrar movimientos", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> resumenPorCategoria() {
        EntityManager em = emf().createEntityManager();
        try {
            String sql = "SELECT categoria, tipo, COALESCE(SUM(monto),0) AS total, COUNT(*) AS cantidad " +
                "FROM movimiento_financiero GROUP BY categoria, tipo ORDER BY categoria, tipo";
            List<Object[]> rows = em.createNativeQuery(sql).getResultList();
            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("categoria", row[0]);
                m.put("tipo", row[1]);
                m.put("total", row[2] != null ? ((Number)row[2]).doubleValue() : 0.0);
                m.put("cantidad", row[3] != null ? ((Number)row[3]).intValue() : 0);
                resultado.add(m);
            }
            return resultado;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener resumen por categoria", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> resumenMensual() {
        EntityManager em = emf().createEntityManager();
        try {
            String sql = "SELECT YEAR(fecha_movimiento) AS anio, MONTH(fecha_movimiento) AS mes, " +
                "COALESCE(SUM(CASE WHEN tipo='INGRESO' THEN monto ELSE 0 END),0) AS ingresos, " +
                "COALESCE(SUM(CASE WHEN tipo='GASTO' THEN monto ELSE 0 END),0) AS gastos " +
                "FROM movimiento_financiero " +
                "GROUP BY YEAR(fecha_movimiento), MONTH(fecha_movimiento) " +
                "ORDER BY anio DESC, mes DESC";
            List<Object[]> rows = em.createNativeQuery(sql).getResultList();
            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("anio", row[0] != null ? ((Number)row[0]).intValue() : 0);
                m.put("mes", row[1] != null ? ((Number)row[1]).intValue() : 0);
                m.put("ingresos", row[2] != null ? ((Number)row[2]).doubleValue() : 0.0);
                m.put("gastos", row[3] != null ? ((Number)row[3]).doubleValue() : 0.0);
                resultado.add(m);
            }
            return resultado;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener resumen mensual", e);
            return List.of();
        } finally {
            em.close();
        }
    }
}
