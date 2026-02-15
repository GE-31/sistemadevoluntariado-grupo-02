package com.sistemadevoluntariado.controller;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Usuario user = (Usuario) req.getSession().getAttribute("usuarioLogeado");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String accion = req.getParameter("accion");
        if ("obtener".equals(accion)) {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            Integer id = parseInteger(req.getParameter("id"));
            if (id == null || id <= 0) {
                JsonObject out = new JsonObject();
                out.addProperty("ok", false);
                out.addProperty("message", "ID de donacion invalido");
                resp.getWriter().write(out.toString());
                return;
            }
            Donacion d = donacionDAO.obtenerPorId(id);
            if (d == null) {
                JsonObject out = new JsonObject();
                out.addProperty("ok", false);
                out.addProperty("message", "Donacion no encontrada");
                resp.getWriter().write(out.toString());
                return;
            }
            resp.getWriter().write(gson.toJson(d));
            return;
        }

        req.setAttribute("page", "donaciones");
        req.setAttribute("donaciones", donacionDAO.listar());
        req.setAttribute("usuario", user);

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

        String accion = req.getParameter("accion");
        if ("anular".equals(accion)) {
            procesarAnulacion(req, resp, user);
            return;
        } else if ("editar".equals(accion)) {
            procesarEdicion(req, resp, user);
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

    private void procesarEdicion(HttpServletRequest req, HttpServletResponse resp, Usuario user)
            throws IOException {
        Integer idDonacion = parseInteger(req.getParameter("idDonacion"));
        if (idDonacion == null || idDonacion <= 0) {
            resp.sendRedirect("donaciones");
            return;
        }

        Donacion actual = donacionDAO.obtenerPorId(idDonacion);
        if (actual == null || "ANULADO".equalsIgnoreCase(actual.getEstado())) {
            resp.sendRedirect("donaciones");
            return;
        }

        Donacion d = new Donacion();
        d.setIdDonacion(idDonacion);
        d.setIdUsuarioRegistro(user.getIdUsuario());

        double nuevaCantidad = parseDouble(req.getParameter("cantidad"));
        if (actual.getIdTipoDonacion() == 1 && nuevaCantidad > 0) {
            d.setCantidad(nuevaCantidad);
        } else {
            d.setCantidad(actual.getCantidad());
        }

        d.setDescripcion(trim(req.getParameter("descripcion")));
        Integer idActividad = parseInteger(req.getParameter("actividad"));
        d.setIdActividad(idActividad != null && idActividad > 0 ? idActividad : actual.getIdActividad());

        boolean donacionAnonima = "1".equals(req.getParameter("donacionAnonima"));
        d.setDonacionAnonima(donacionAnonima);
        if (!donacionAnonima) {
            d.setTipoDonante(trim(req.getParameter("tipoDonante")));
            d.setNombreDonante(trim(req.getParameter("nombreDonante")));
            d.setCorreoDonante(trim(req.getParameter("correoDonante")));
            d.setTelefonoDonante(trim(req.getParameter("telefonoDonante")));
        }
        d.setMotivoAnulacion(trim(req.getParameter("motivoEdicion")));

        donacionDAO.actualizar(d);
        resp.sendRedirect("donaciones");
    }

    private void procesarAnulacion(HttpServletRequest req, HttpServletResponse resp, Usuario user)
            throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        JsonObject out = new JsonObject();

        Integer idDonacion = parseInteger(req.getParameter("idDonacion"));
        if (idDonacion == null || idDonacion <= 0) {
            out.addProperty("ok", false);
            out.addProperty("message", "ID de donacion invalido");
            resp.getWriter().write(out.toString());
            return;
        }

        String motivo = trim(req.getParameter("motivo"));
        if (motivo == null || motivo.isEmpty()) {
            motivo = "Anulacion manual";
        }

        boolean ok = donacionDAO.anular(idDonacion, user.getIdUsuario(), motivo);
        out.addProperty("ok", ok);
        out.addProperty("message", ok ? "Donacion anulada" : "No se pudo anular la donacion");
        resp.getWriter().write(out.toString());
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
