package com.sistemadevoluntariado.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.ConexionBD;
import com.sistemadevoluntariado.model.InventarioItem;

public class InventarioDAO {

    private static final Logger logger = Logger.getLogger(InventarioDAO.class.getName());
    private final Connection cx;

    public InventarioDAO() {
        cx = ConexionBD.getInstance().getConnection();
    }

    public List<InventarioItem> listar() {
        List<InventarioItem> lista = new ArrayList<>();
        try (CallableStatement cs = cx.prepareCall("{CALL sp_listar_inventario()}");
                ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al listar inventario", e);
        }
        return lista;
    }

    public List<InventarioItem> filtrar(String q, String categoria, String estado, boolean stockBajo) {
        List<InventarioItem> lista = new ArrayList<>();
        try (CallableStatement cs = cx.prepareCall("{CALL sp_filtrar_inventario(?,?,?,?)}")) {
            cs.setString(1, q);
            cs.setString(2, categoria);
            cs.setString(3, estado);
            cs.setInt(4, stockBajo ? 1 : 0);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al filtrar inventario", e);
        }
        return lista;
    }

    public InventarioItem obtenerPorId(int id) {
        try (CallableStatement cs = cx.prepareCall("{CALL sp_obtener_item_inventario(?)}")) {
            cs.setInt(1, id);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener item inventario", e);
        }
        return null;
    }

    public boolean registrar(InventarioItem item) {
        try (CallableStatement cs = cx.prepareCall("{CALL sp_crear_item_inventario(?,?,?,?,?)}")) {
            cs.setString(1, item.getNombre());
            cs.setString(2, item.getCategoria());
            cs.setString(3, item.getUnidadMedida());
            cs.setDouble(4, item.getStockMinimo());
            cs.setString(5, item.getObservacion());
            cs.execute();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al registrar item inventario", e);
            return false;
        }
    }

    public boolean actualizar(InventarioItem item) {
        try (CallableStatement cs = cx.prepareCall("{CALL sp_actualizar_item_inventario(?,?,?,?,?,?)}")) {
            cs.setInt(1, item.getIdItem());
            cs.setString(2, item.getNombre());
            cs.setString(3, item.getCategoria());
            cs.setString(4, item.getUnidadMedida());
            cs.setDouble(5, item.getStockMinimo());
            cs.setString(6, item.getObservacion());
            cs.execute();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al actualizar item inventario", e);
            return false;
        }
    }

    public boolean registrarMovimiento(int idItem, String tipo, String motivo, double cantidad, String observacion, int idUsuario) {
        try (CallableStatement cs = cx.prepareCall("{CALL sp_registrar_movimiento_inventario(?,?,?,?,?,?)}")) {
            cs.setInt(1, idItem);
            cs.setString(2, tipo);
            cs.setString(3, motivo);
            cs.setDouble(4, cantidad);
            cs.setString(5, observacion);
            cs.setInt(6, idUsuario);
            cs.execute();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al registrar movimiento inventario", e);
            return false;
        }
    }

    public boolean cambiarEstado(int idItem, String estado) {
        try (CallableStatement cs = cx.prepareCall("{CALL sp_cambiar_estado_inventario(?,?)}")) {
            cs.setInt(1, idItem);
            cs.setString(2, estado != null ? estado : "INACTIVO");
            cs.execute();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al cambiar estado inventario", e);
            return false;
        }
    }

    public int contarStockBajo() {
        try (CallableStatement cs = cx.prepareCall("{CALL sp_contar_stock_bajo()}");
                ResultSet rs = cs.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al contar stock bajo", e);
        }
        return 0;
    }

    private InventarioItem mapear(ResultSet rs) throws SQLException {
        InventarioItem item = new InventarioItem();
        item.setIdItem(rs.getInt("id_item"));
        item.setNombre(rs.getString("nombre"));
        item.setCategoria(rs.getString("categoria"));
        item.setUnidadMedida(rs.getString("unidad_medida"));
        item.setStockActual(rs.getDouble("stock_actual"));
        item.setStockMinimo(rs.getDouble("stock_minimo"));
        item.setEstado(rs.getString("estado"));
        item.setObservacion(rs.getString("observacion"));
        item.setCreadoEn(rs.getString("creado_en"));
        item.setActualizadoEn(rs.getString("actualizado_en"));
        return item;
    }
}
