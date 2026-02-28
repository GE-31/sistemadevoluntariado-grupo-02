package com.sistemadevoluntariado.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.PersistenceManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class DashboardRepository {

    private static final Logger logger = Logger.getLogger(DashboardRepository.class.getName());
    private EntityManagerFactory emf() {
        return PersistenceManager.getEntityManagerFactory();
    }

    /**
     * Obtener actividades por mes (últimos 6 meses)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerActividadesPorMes() {
        Map<String, Object> resultado = new LinkedHashMap<>();
        List<String> labels = new ArrayList<>();
        List<Integer> data = new ArrayList<>();

        EntityManager em = emf().createEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                "SELECT DATE_FORMAT(m.mes, '%b') AS nombre_mes, " +
                "IFNULL(COUNT(a.id_actividad), 0) AS total_actividades " +
                "FROM ( " +
                "  SELECT DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL n MONTH), '%Y-%m-01') AS mes " +
                "  FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 " +
                "        UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) nums " +
                ") m " +
                "LEFT JOIN actividades a " +
                "  ON DATE_FORMAT(a.fecha_inicio, '%Y-%m') = DATE_FORMAT(m.mes, '%Y-%m') " +
                "GROUP BY m.mes ORDER BY m.mes ASC")
                .getResultList();
            for (Object[] row : rows) {
                labels.add((String) row[0]);
                data.add(row[1] != null ? ((Number) row[1]).intValue() : 0);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error sp_actividades_por_mes", e);
        } finally {
            em.close();
        }

        resultado.put("labels", labels);
        resultado.put("data", data);
        return resultado;
    }

    /**
     * Horas voluntarias agrupadas por actividad (top 5)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerHorasVoluntariasPorActividad() {
        Map<String, Object> resultado = new LinkedHashMap<>();
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        EntityManager em = emf().createEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                "SELECT act.nombre AS nombre_actividad, " +
                "IFNULL(SUM(a.horas_totales), 0) AS total_horas " +
                "FROM asistencias a " +
                "INNER JOIN actividades act ON a.id_actividad = act.id_actividad " +
                "WHERE a.estado IN ('ASISTIO', 'TARDANZA') " +
                "GROUP BY act.id_actividad, act.nombre " +
                "ORDER BY total_horas DESC LIMIT 5")
                .getResultList();
            for (Object[] row : rows) {
                labels.add((String) row[0]);
                data.add(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error sp_horas_voluntarias_por_actividad", e);
        } finally {
            em.close();
        }

        resultado.put("labels", labels);
        resultado.put("data", data);
        return resultado;
    }

    /**
     * Total global de horas voluntarias
     */
    public double obtenerTotalHorasVoluntarias() {
        EntityManager em = emf().createEntityManager();
        try {
            Object result = em.createNativeQuery(
                "SELECT IFNULL(SUM(horas_totales), 0) FROM asistencias " +
                "WHERE estado IN ('ASISTIO', 'TARDANZA')")
                .getSingleResult();
            return result != null ? ((Number) result).doubleValue() : 0;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error sp_total_horas_voluntarias", e);
            return 0;
        } finally {
            em.close();
        }
    }

    /**
     * Próxima actividad programada
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> obtenerProximaActividad() {
        EntityManager em = emf().createEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                "SELECT nombre, fecha_inicio, ubicacion FROM actividades " +
                "WHERE fecha_inicio >= CURDATE() AND estado = 'ACTIVO' " +
                "ORDER BY fecha_inicio ASC LIMIT 1")
                .getResultList();
            if (!rows.isEmpty()) {
                Object[] row = rows.get(0);
                Map<String, String> actividad = new LinkedHashMap<>();
                actividad.put("nombre",    row[0] != null ? row[0].toString() : "");
                actividad.put("fecha",     row[1] != null ? row[1].toString() : "");
                actividad.put("ubicacion", row[2] != null ? row[2].toString() : "");
                return actividad;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error sp_proxima_actividad", e);
        } finally {
            em.close();
        }
        return null;
    }
}
 