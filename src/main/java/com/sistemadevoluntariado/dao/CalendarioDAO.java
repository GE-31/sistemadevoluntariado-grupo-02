package com.sistemadevoluntariado.dao;


import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.sistemadevoluntariado.config.ConexionBD;
import com.sistemadevoluntariado.model.Calendario;

public class CalendarioDAO {

    // GUARDAR EVENTO (SP)
    public boolean crearEvento(Calendario c) {
        String sql = "{CALL sp_crear_evento(?,?,?,?,?,?)}";

        try {
            Connection con = ConexionBD.getInstance().getConnection();
            CallableStatement cs = con.prepareCall(sql);
            cs.setString(1, c.getTitulo());
            cs.setString(2, c.getDescripcion());
            cs.setString(3, c.getFechaInicio());
            cs.setString(4, c.getFechaFin());
            cs.setString(5, c.getColor() != null ? c.getColor() : "#6366f1");
            cs.setInt(6, c.getIdUsuario());
            return cs.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // LISTAR EVENTOS (SP)
    public List<Calendario> listarEventos() {
        List<Calendario> lista = new ArrayList<>();
        String sql = "{CALL sp_listar_eventos()}";

        try {
            Connection con = ConexionBD.getInstance().getConnection();
            CallableStatement cs = con.prepareCall(sql);
            ResultSet rs = cs.executeQuery();
            while (rs.next()) {
                Calendario c = new Calendario();
                c.setIdEvento(rs.getInt("id_evento"));
                c.setTitulo(rs.getString("titulo"));
                c.setDescripcion(rs.getString("descripcion"));
                c.setFechaInicio(rs.getString("fecha_inicio"));
                c.setFechaFin(rs.getString("fecha_fin"));
                try { c.setColor(rs.getString("color")); } catch (Exception ignored) {}
                lista.add(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }
}

