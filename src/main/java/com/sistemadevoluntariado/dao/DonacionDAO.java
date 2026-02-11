package com.sistemadevoluntariado.dao;


import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.sistemadevoluntariado.config.ConexionBD;
import com.sistemadevoluntariado.model.Donacion;

public class DonacionDAO {

    private Connection cx;

    public DonacionDAO() {
        cx = ConexionBD.getInstance().getConnection();
    }

    // LISTAR
    public List<Donacion> listar() {
        List<Donacion> lista = new ArrayList<>();
        try {
            CallableStatement cs = cx.prepareCall("{CALL sp_listarDonaciones()}");
            ResultSet rs = cs.executeQuery();

            while (rs.next()) {
                Donacion d = new Donacion();
                d.setIdDonacion(rs.getInt("id_donacion"));
                d.setCantidad(rs.getDouble("cantidad"));
                d.setDescripcion(rs.getString("descripcion"));
                d.setTipoDonacion(rs.getString("tipoDonacion"));
                d.setActividad(rs.getString("actividad"));
                d.setUsuarioRegistro(rs.getString("usuarioRegistro"));
                d.setRegistradoEn(rs.getString("registrado_en"));
                lista.add(d);
            }
        } catch (Exception e) { e.printStackTrace(); }

        return lista;
    }

    // GUARDAR
    public boolean guardar(Donacion d) {
        try {
            CallableStatement cs = cx.prepareCall("{CALL sp_guardarDonacion(?,?,?,?,?)}");
            cs.setDouble(1, d.getCantidad());
            cs.setString(2, d.getDescripcion());
            cs.setInt(3, d.getIdTipoDonacion());
            cs.setInt(4, d.getIdActividad());
            cs.setInt(5, d.getIdUsuarioRegistro());
            cs.execute();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
