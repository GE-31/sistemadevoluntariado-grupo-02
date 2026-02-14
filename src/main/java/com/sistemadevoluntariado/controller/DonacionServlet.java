package com.sistemadevoluntariado.controller;

import java.io.IOException;

import com.sistemadevoluntariado.dao.DonacionDAO;
import com.sistemadevoluntariado.model.Donacion;
import com.sistemadevoluntariado.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/donaciones")
public class DonacionServlet extends HttpServlet {

    DonacionDAO donacionDAO = new DonacionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("page", "donaciones");
        req.setAttribute("donaciones", donacionDAO.listar());

        req.getRequestDispatcher("/views/donaciones/donaciones.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Usuario user = (Usuario) req.getSession().getAttribute("usuarioLogeado");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Donacion d = new Donacion();
        double cantidad = parseDouble(req.getParameter("cantidad"));
        if (cantidad <= 0) {
            resp.sendRedirect("donaciones");
            return;
        }
        d.setCantidad(cantidad);
        d.setDescripcion(req.getParameter("descripcion"));
        int idTipoDonacion = Integer.parseInt(req.getParameter("tipoDonacion"));
        d.setIdTipoDonacion(idTipoDonacion);
        d.setIdActividad(Integer.parseInt(req.getParameter("actividad")));
        d.setIdUsuarioRegistro(user.getIdUsuario());
        boolean donacionAnonima = "1".equals(req.getParameter("donacionAnonima"));
        d.setDonacionAnonima(donacionAnonima);
        if (!donacionAnonima) {
            d.setTipoDonante(trim(req.getParameter("tipoDonante")));
            d.setNombreDonante(trim(req.getParameter("nombreDonante")));
            d.setCorreoDonante(trim(req.getParameter("correoDonante")));
            d.setTelefonoDonante(trim(req.getParameter("telefonoDonante")));
        }

        if (idTipoDonacion == 2) {
            Integer idItem = parseInteger(req.getParameter("idItem"));
            d.setIdItem(idItem);
            boolean crearNuevoItem = "1".equals(req.getParameter("crearNuevoItem"));
            d.setCrearNuevoItem(crearNuevoItem);
            if (crearNuevoItem || idItem == null || idItem <= 0) {
                d.setItemNombre(trim(req.getParameter("itemNombre")));
                d.setItemCategoria(trim(req.getParameter("itemCategoria")));
                d.setItemUnidadMedida(trim(req.getParameter("itemUnidadMedida")));
                d.setItemStockMinimo(parseDouble(req.getParameter("itemStockMinimo")));
            }
        }

        donacionDAO.guardar(d);
        resp.sendRedirect("donaciones");
    }

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0d;
        }
        return Double.parseDouble(value);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Integer.parseInt(value);
    }
}
