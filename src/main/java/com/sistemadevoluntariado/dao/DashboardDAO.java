package com.sistemadevoluntariado.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.ConexionBD;

public class DashboardDAO {

    private static final Logger logger = Logger.getLogger(DashboardDAO.class.getName());

    /**
     * Obtener actividades por mes (últimos 6 meses)
     * Retorna un Map<String, Object[]> con clave "labels" y "data"
     */
    public Map<String, Object> obtenerActividadesPorMes() {
        Map<String, Object> resultado = new LinkedHashMap<>();
        List<String> labels = new ArrayList<>();
        List<Integer> data = new ArrayList<>();

        String sql = "{CALL sp_actividades_por_mes()}";

        try {
            Connection conn = ConexionBD.getInstance().getConnection();
            if (conn == null) { logger.severe("✗ Sin conexión"); return resultado; }

            try (CallableStatement cs = conn.prepareCall(sql);
                 ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    labels.add(rs.getString("nombre_mes"));
                    data.add(rs.getInt("total_actividades"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error sp_actividades_por_mes", e);
        }

        resultado.put("labels", labels);
        resultado.put("data", data);
        return resultado;
    }

    /**
     * Obtener horas voluntarias agrupadas por actividad (top 5)
     */
    public Map<String, Object> obtenerHorasVoluntariasPorActividad() {
        Map<String, Object> resultado = new LinkedHashMap<>();
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        String sql = "{CALL sp_horas_voluntarias_por_actividad()}";

        try {
            Connection conn = ConexionBD.getInstance().getConnection();
            if (conn == null) { logger.severe("✗ Sin conexión"); return resultado; }

            try (CallableStatement cs = conn.prepareCall(sql);
                 ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    labels.add(rs.getString("nombre_actividad"));
                    data.add(rs.getDouble("total_horas"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error sp_horas_voluntarias_por_actividad", e);
        }

        resultado.put("labels", labels);
        resultado.put("data", data);
        return resultado;
    }

    /**
     * Total global de horas voluntarias
     */
    public double obtenerTotalHorasVoluntarias() {
        String sql = "{CALL sp_total_horas_voluntarias()}";

        try {
            Connection conn = ConexionBD.getInstance().getConnection();
            if (conn == null) return 0;

            try (CallableStatement cs = conn.prepareCall(sql);
                 ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_horas");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error sp_total_horas_voluntarias", e);
        }
        return 0;
    }

    /**
     * Próxima actividad programada
     * Retorna Map con nombre, fecha, ubicacion o null
     */
    public Map<String, String> obtenerProximaActividad() {
        String sql = "{CALL sp_proxima_actividad()}";

        try {
            Connection conn = ConexionBD.getInstance().getConnection();
            if (conn == null) return null;

            try (CallableStatement cs = conn.prepareCall(sql);
                 ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> actividad = new LinkedHashMap<>();
                    actividad.put("nombre", rs.getString("nombre"));
                    actividad.put("fecha", rs.getString("fecha_inicio"));
                    actividad.put("ubicacion", rs.getString("ubicacion"));
                    return actividad;
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "✗ Error sp_proxima_actividad", e);
        }
        return null;
    }
}
