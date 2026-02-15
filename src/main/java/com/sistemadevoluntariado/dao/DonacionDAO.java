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
                d.setEstado(rs.getString("estado"));
                lista.add(d);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public Donacion obtenerPorId(int idDonacion) {
        try (CallableStatement cs = cx.prepareCall("{CALL sp_obtener_donacion_detalle(?)}")) {
            cs.setInt(1, idDonacion);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    Donacion d = new Donacion();
                    d.setIdDonacion(rs.getInt("id_donacion"));
                    d.setCantidad(rs.getDouble("cantidad"));
                    d.setDescripcion(rs.getString("descripcion"));
                    d.setIdTipoDonacion(rs.getInt("id_tipo_donacion"));
                    d.setTipoDonacion(rs.getString("tipoDonacion"));
                    d.setIdActividad(rs.getInt("id_actividad"));
                    d.setActividad(rs.getString("actividad"));
                    d.setIdUsuarioRegistro(rs.getInt("id_usuario_registro"));
                    d.setUsuarioRegistro(rs.getString("usuarioRegistro"));
                    d.setDonacionAnonima(rs.getInt("donacion_anonima") == 1);
                    d.setTipoDonante(rs.getString("tipo_donante"));
                    d.setNombreDonante(rs.getString("nombre_donante"));
                    d.setCorreoDonante(rs.getString("correo_donante"));
                    d.setTelefonoDonante(rs.getString("telefono_donante"));
                    d.setEstado(rs.getString("estado"));

                    int idItem = rs.getInt("id_item");
                    if (!rs.wasNull()) {
                        d.setIdItem(idItem);
                    }
                    d.setItemNombre(rs.getString("item_nombre"));
                    d.setItemCategoria(rs.getString("item_categoria"));
                    d.setItemUnidadMedida(rs.getString("item_unidad_medida"));
                    d.setCantidadItem(rs.getDouble("cantidad_item"));
                    if (rs.wasNull()) {
                        d.setCantidadItem(null);
                    }
                    return d;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    public boolean actualizar(Donacion d) {
        try (CallableStatement cs = cx.prepareCall("{CALL sp_actualizar_donacion_inventario(?,?,?,?,?,?,?,?,?,?,?)}")) {
            cs.setInt(1, d.getIdDonacion());
            cs.setDouble(2, d.getCantidad() != null ? d.getCantidad() : 0d);
            cs.setString(3, d.getDescripcion());
            cs.setInt(4, d.getIdActividad());
            cs.setInt(5, d.isDonacionAnonima() ? 1 : 0);
            cs.setString(6, d.getTipoDonante());
            cs.setString(7, d.getNombreDonante());
            cs.setString(8, d.getCorreoDonante());
            cs.setString(9, d.getTelefonoDonante());
            cs.setInt(10, d.getIdUsuarioRegistro());
            cs.setString(11, d.getMotivoAnulacion());
            cs.execute();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean anular(int idDonacion, int idUsuario, String motivo) {
        try (CallableStatement cs = cx.prepareCall("{CALL sp_anular_donacion_inventario(?,?,?)}")) {
            cs.setInt(1, idDonacion);
            cs.setInt(2, idUsuario);
            cs.setString(3, motivo);
            cs.execute();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
