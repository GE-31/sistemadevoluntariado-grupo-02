package com.sistemadevoluntariado.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.sistemadevoluntariado.config.ConexionBD;
import com.sistemadevoluntariado.model.Donacion;

public class DonacionDAO {

    private final Connection cx;

    public DonacionDAO() {
        cx = ConexionBD.getInstance().getConnection();
    }

    public List<Donacion> listar() {
        List<Donacion> lista = new ArrayList<>();
        try (CallableStatement cs = cx.prepareCall("{CALL sp_listar_donaciones_con_detalle()}");
                ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                Donacion d = new Donacion();
                d.setIdDonacion(rs.getInt("id_donacion"));
                d.setCantidad(rs.getDouble("cantidad"));
                d.setDescripcion(rs.getString("descripcion"));
                d.setTipoDonacion(rs.getString("tipoDonacion"));
                d.setActividad(rs.getString("actividad"));
                d.setUsuarioRegistro(rs.getString("usuarioRegistro"));
                d.setDonanteNombre(rs.getString("donanteNombre"));
                d.setRegistradoEn(rs.getString("registrado_en"));
                int idItem = rs.getInt("id_item");
                if (!rs.wasNull()) {
                    d.setIdItem(idItem);
                }
                double cantidadItem = rs.getDouble("cantidad_item");
                if (!rs.wasNull()) {
                    d.setCantidadItem(cantidadItem);
                }
                d.setItemNombre(rs.getString("item_nombre"));
                d.setItemUnidadMedida(rs.getString("item_unidad_medida"));
                lista.add(d);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public boolean guardar(Donacion d) {
        try (CallableStatement cs = cx.prepareCall("{CALL sp_registrar_donacion_inventario(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}")) {
            cs.setDouble(1, d.getCantidad() != null ? d.getCantidad() : 0d);
            cs.setString(2, d.getDescripcion());
            cs.setInt(3, d.getIdTipoDonacion());
            cs.setInt(4, d.getIdActividad());
            cs.setInt(5, d.getIdUsuarioRegistro());
            if (d.getIdItem() == null) {
                cs.setNull(6, java.sql.Types.INTEGER);
            } else {
                cs.setInt(6, d.getIdItem());
            }
            cs.setInt(7, d.isCrearNuevoItem() ? 1 : 0);
            cs.setString(8, d.getItemNombre());
            cs.setString(9, d.getItemCategoria());
            cs.setString(10, d.getItemUnidadMedida());
            cs.setDouble(11, d.getItemStockMinimo() != null ? d.getItemStockMinimo() : 0d);
            cs.setInt(12, d.isDonacionAnonima() ? 1 : 0);
            cs.setString(13, d.getTipoDonante());
            cs.setString(14, d.getNombreDonante());
            cs.setString(15, d.getCorreoDonante());
            cs.setString(16, d.getTelefonoDonante());
            cs.execute();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
