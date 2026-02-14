package com.sistemadevoluntariado.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.ConexionBD;
import com.sistemadevoluntariado.model.MovimientoFinanciero;

public class TesoreriaDAO {

    private static final Logger logger = Logger.getLogger(TesoreriaDAO.class.getName());
    private Connection cx;

    public TesoreriaDAO() {
        cx = ConexionBD.getInstance().getConnection();
    }

    /**
     * Lista todos los movimientos financieros.
     */
    public List<MovimientoFinanciero> listar() {
        List<MovimientoFinanciero> lista = new ArrayList<>();
        try {
            CallableStatement cs = cx.prepareCall("{CALL sp_listarMovimientos()}");
            ResultSet rs = cs.executeQuery();
            while (rs.next()) {
                lista.add(mapearMovimiento(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al listar movimientos", e);
        }
        return lista;
    }

    /**
     * Obtiene un movimiento por su ID.
     */
    public MovimientoFinanciero obtenerPorId(int id) {
        try {
            CallableStatement cs = cx.prepareCall("{CALL sp_obtenerMovimiento(?)}");
            cs.setInt(1, id);
            ResultSet rs = cs.executeQuery();
            if (rs.next()) {
                MovimientoFinanciero m = new MovimientoFinanciero();
                m.setIdMovimiento(rs.getInt("id_movimiento"));
                m.setTipo(rs.getString("tipo"));
                m.setMonto(rs.getDouble("monto"));
                m.setDescripcion(rs.getString("descripcion"));
                m.setCategoria(rs.getString("categoria"));
                m.setComprobante(rs.getString("comprobante"));
                m.setFechaMovimiento(rs.getString("fecha_movimiento"));
                m.setIdActividad(rs.getInt("id_actividad"));
                m.setIdUsuario(rs.getInt("id_usuario"));
                m.setCreadoEn(rs.getString("creado_en"));
                return m;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener movimiento", e);
        }
        return null;
    }

    /**
     * Registra un nuevo movimiento financiero.
     */
    public boolean registrar(MovimientoFinanciero m) {
        try {
            CallableStatement cs = cx.prepareCall("{CALL sp_registrarMovimiento(?,?,?,?,?,?,?,?)}");
            cs.setString(1, m.getTipo());
            cs.setDouble(2, m.getMonto());
            cs.setString(3, m.getDescripcion());
            cs.setString(4, m.getCategoria());
            cs.setString(5, m.getComprobante());
            cs.setString(6, m.getFechaMovimiento());
            cs.setInt(7, m.getIdActividad());
            cs.setInt(8, m.getIdUsuario());
            cs.execute();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al registrar movimiento", e);
            return false;
        }
    }

    /**
     * Actualiza un movimiento existente.
     */
    public boolean actualizar(MovimientoFinanciero m) {
        try {
            CallableStatement cs = cx.prepareCall("{CALL sp_actualizarMovimiento(?,?,?,?,?,?,?,?)}");
            cs.setInt(1, m.getIdMovimiento());
            cs.setString(2, m.getTipo());
            cs.setDouble(3, m.getMonto());
            cs.setString(4, m.getDescripcion());
            cs.setString(5, m.getCategoria());
            cs.setString(6, m.getComprobante());
            cs.setString(7, m.getFechaMovimiento());
            cs.setInt(8, m.getIdActividad());
            cs.execute();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al actualizar movimiento", e);
            return false;
        }
    }

    /**
     * Elimina un movimiento por su ID.
     */
    public boolean eliminar(int id) {
        try {
            CallableStatement cs = cx.prepareCall("{CALL sp_eliminarMovimiento(?)}");
            cs.setInt(1, id);
            cs.execute();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al eliminar movimiento", e);
            return false;
        }
    }

    /**
     * Obtiene el balance general: total ingresos, total gastos, saldo.
     */
    public Map<String, Double> obtenerBalance() {
        Map<String, Double> balance = new HashMap<>();
        balance.put("ingresos", 0.0);
        balance.put("gastos", 0.0);
        balance.put("saldo", 0.0);

        try {
            CallableStatement cs = cx.prepareCall("{CALL sp_obtenerBalance()}");
            ResultSet rs = cs.executeQuery();
            if (rs.next()) {
                balance.put("ingresos", rs.getDouble("total_ingresos"));
                balance.put("gastos", rs.getDouble("total_gastos"));
                balance.put("saldo", rs.getDouble("saldo"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener balance", e);
        }
        return balance;
    }

    /**
     * Filtra movimientos por tipo, categoría y rango de fechas.
     */
    public List<MovimientoFinanciero> filtrar(String tipo, String categoria,
            String fechaIni, String fechaFin) {
        List<MovimientoFinanciero> lista = new ArrayList<>();
        try {
            CallableStatement cs = cx.prepareCall("{CALL sp_filtrarMovimientos(?,?,?,?)}");
            cs.setString(1, tipo != null && !tipo.isEmpty() ? tipo : null);
            cs.setString(2, categoria != null && !categoria.isEmpty() ? categoria : null);
            cs.setString(3, fechaIni != null && !fechaIni.isEmpty() ? fechaIni : null);
            cs.setString(4, fechaFin != null && !fechaFin.isEmpty() ? fechaFin : null);
            ResultSet rs = cs.executeQuery();
            while (rs.next()) {
                lista.add(mapearMovimiento(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al filtrar movimientos", e);
        }
        return lista;
    }

    /**
     * Resumen por categoría para gráficos.
     */
    public List<Map<String, Object>> resumenPorCategoria() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            CallableStatement cs = cx.prepareCall("{CALL sp_resumenPorCategoria()}");
            ResultSet rs = cs.executeQuery();
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("categoria", rs.getString("categoria"));
                item.put("tipo", rs.getString("tipo"));
                item.put("total", rs.getDouble("total"));
                item.put("cantidad", rs.getInt("cantidad"));
                lista.add(item);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener resumen por categoría", e);
        }
        return lista;
    }

    /**
     * Resumen mensual para gráficos de tendencia.
     */
    public List<Map<String, Object>> resumenMensual() {
        List<Map<String, Object>> lista = new ArrayList<>();
        try {
            CallableStatement cs = cx.prepareCall("{CALL sp_resumenMensual()}");
            ResultSet rs = cs.executeQuery();
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("mes", rs.getString("mes"));
                item.put("ingresos", rs.getDouble("ingresos"));
                item.put("gastos", rs.getDouble("gastos"));
                lista.add(item);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener resumen mensual", e);
        }
        return lista;
    }

    /**
     * Mapea un ResultSet a un objeto MovimientoFinanciero (para listados).
     */
    private MovimientoFinanciero mapearMovimiento(ResultSet rs) throws SQLException {
        MovimientoFinanciero m = new MovimientoFinanciero();
        m.setIdMovimiento(rs.getInt("id_movimiento"));
        m.setTipo(rs.getString("tipo"));
        m.setMonto(rs.getDouble("monto"));
        m.setDescripcion(rs.getString("descripcion"));
        m.setCategoria(rs.getString("categoria"));
        m.setComprobante(rs.getString("comprobante"));
        m.setFechaMovimiento(rs.getString("fecha_movimiento"));
        m.setActividad(rs.getString("actividad"));
        m.setIdActividad(rs.getInt("id_actividad"));
        m.setUsuarioRegistro(rs.getString("usuario_registro"));
        m.setCreadoEn(rs.getString("creado_en"));
        return m;
    }
}
