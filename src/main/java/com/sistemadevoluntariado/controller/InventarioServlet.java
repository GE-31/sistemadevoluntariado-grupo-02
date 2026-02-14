package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sistemadevoluntariado.dao.InventarioDAO;
import com.sistemadevoluntariado.model.InventarioItem;
import com.sistemadevoluntariado.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/inventario")
public class InventarioServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final InventarioDAO inventarioDAO = new InventarioDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuarioLogeado") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogeado");
        req.setAttribute("usuario", usuario);

        String accion = req.getParameter("accion");

        if (accion != null) {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            switch (accion) {
                case "listar":
                    resp.getWriter().write(gson.toJson(inventarioDAO.listar()));
                    return;
                case "obtener":
                    int id = Integer.parseInt(req.getParameter("id"));
                    resp.getWriter().write(gson.toJson(inventarioDAO.obtenerPorId(id)));
                    return;
                case "filtrar":
                    String q = req.getParameter("q");
                    String categoria = req.getParameter("categoria");
                    String estado = req.getParameter("estado");
                    boolean stockBajo = "1".equals(req.getParameter("stockBajo"));
                    List<InventarioItem> filtrados = inventarioDAO.filtrar(q, categoria, estado, stockBajo);
                    resp.getWriter().write(gson.toJson(filtrados));
                    return;
                case "stock_bajo":
                    JsonObject json = new JsonObject();
                    json.addProperty("total", inventarioDAO.contarStockBajo());
                    resp.getWriter().write(json.toString());
                    return;
                default:
                    break;
            }
        }

        req.setAttribute("page", "inventario");
        req.setAttribute("items", inventarioDAO.listar());
        req.setAttribute("stockBajoTotal", inventarioDAO.contarStockBajo());
        req.getRequestDispatcher("/views/inventario/inventario.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuarioLogeado") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String accion = req.getParameter("accion");
        JsonObject out = new JsonObject();

        try {
            if ("registrar".equals(accion)) {
                InventarioItem item = construirDesdeRequest(req);
                boolean ok = inventarioDAO.registrar(item);
                out.addProperty("ok", ok);
                out.addProperty("message", ok ? "Item registrado correctamente" : "No se pudo registrar el item");
            } else if ("actualizar".equals(accion)) {
                InventarioItem item = construirDesdeRequest(req);
                item.setIdItem(Integer.parseInt(req.getParameter("idItem")));
                boolean ok = inventarioDAO.actualizar(item);
                out.addProperty("ok", ok);
                out.addProperty("message", ok ? "Item actualizado correctamente" : "No se pudo actualizar el item");
            } else if ("cambiar_estado".equals(accion)) {
                int idItem = Integer.parseInt(req.getParameter("idItem"));
                String estado = req.getParameter("estado");
                boolean ok = inventarioDAO.cambiarEstado(idItem, estado);
                out.addProperty("ok", ok);
                out.addProperty("message", ok ? "Estado actualizado" : "No se pudo actualizar el estado");
            } else if ("registrar_movimiento".equals(accion)) {
                int idItem = Integer.parseInt(req.getParameter("idItem"));
                String tipo = req.getParameter("tipoMovimiento");
                String motivo = req.getParameter("motivo");
                double cantidad = parseDouble(req.getParameter("cantidad"));
                String observacion = req.getParameter("observacion");
                Usuario usuario = (Usuario) session.getAttribute("usuarioLogeado");
                boolean ok = inventarioDAO.registrarMovimiento(idItem, tipo, motivo, cantidad, observacion, usuario.getIdUsuario());
                out.addProperty("ok", ok);
                out.addProperty("message", ok ? "Movimiento registrado correctamente" : "No se pudo registrar el movimiento");
            } else {
                out.addProperty("ok", false);
                out.addProperty("message", "Accion no valida");
            }
        } catch (Exception e) {
            out.addProperty("ok", false);
            out.addProperty("message", "Error: " + e.getMessage());
        }

        resp.getWriter().write(out.toString());
    }

    private InventarioItem construirDesdeRequest(HttpServletRequest req) {
        InventarioItem item = new InventarioItem();
        item.setNombre(req.getParameter("nombre"));
        item.setCategoria(req.getParameter("categoria"));
        item.setUnidadMedida(req.getParameter("unidadMedida"));
        item.setStockMinimo(parseDouble(req.getParameter("stockMinimo")));
        item.setObservacion(req.getParameter("observacion"));
        return item;
    }

    private double parseDouble(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return 0;
        }
        return Double.parseDouble(valor);
    }
}
