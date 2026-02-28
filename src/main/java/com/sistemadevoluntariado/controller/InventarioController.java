package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sistemadevoluntariado.entity.CategoriaInventario;
import com.sistemadevoluntariado.entity.InventarioItem;
import com.sistemadevoluntariado.entity.Usuario;
import com.sistemadevoluntariado.repository.InventarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/inventario")
public class InventarioController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private InventarioRepository InventarioRepository;
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (InventarioRepository == null) InventarioRepository = new InventarioRepository();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuarioLogeado") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogeado");
        req.setAttribute("usuario", usuario);

        String accion = req.getParameter("accion");

        if (accion != null) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");

            switch (accion) {
                case "listar_categorias":
                    resp.getWriter().write(gson.toJson(InventarioRepository.listarCategorias()));
                    return;
                case "listar":
                    resp.getWriter().write(gson.toJson(InventarioRepository.listar()));
                    return;
                case "obtener":
                    int id = Integer.parseInt(req.getParameter("id"));
                    resp.getWriter().write(gson.toJson(InventarioRepository.obtenerPorId(id)));
                    return;
                case "filtrar":
                    String q = req.getParameter("q");
                    String categoria = req.getParameter("categoria");
                    String estado = req.getParameter("estado");
                    boolean stockBajo = "1".equals(req.getParameter("stockBajo"));
                    List<InventarioItem> filtrados = InventarioRepository.filtrar(q, categoria, estado, stockBajo);
                    resp.getWriter().write(gson.toJson(filtrados));
                    return;
                case "stock_bajo":
                    JsonObject json = new JsonObject();
                    json.addProperty("total", InventarioRepository.contarStockBajo());
                    resp.getWriter().write(json.toString());
                    return;
                default:
                    break;
            }
        }

        req.setAttribute("page", "inventario");
        req.setAttribute("categorias", InventarioRepository.listarCategorias());
        req.setAttribute("items", InventarioRepository.listar());
        req.setAttribute("stockBajoTotal", InventarioRepository.contarStockBajo());
        req.getRequestDispatcher("/views/inventario/inventario.html").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (InventarioRepository == null) InventarioRepository = new InventarioRepository();

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("usuarioLogeado") == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String accion = req.getParameter("accion");
        JsonObject out = new JsonObject();

        try {
            // ── Categorías ──────────────────────────────────────────────────
            if ("registrar_categoria".equals(accion)) {
                CategoriaInventario cat = construirCategoriaDesdeRequest(req);
                boolean ok = InventarioRepository.registrarCategoria(cat);
                out.addProperty("ok", ok);
                out.addProperty("message", ok ? "Categoría registrada correctamente" : "No se pudo registrar la categoría");
            } else if ("actualizar_categoria".equals(accion)) {
                CategoriaInventario cat = construirCategoriaDesdeRequest(req);
                cat.setIdCategoria(Integer.parseInt(req.getParameter("idCategoria")));
                boolean ok = InventarioRepository.actualizarCategoria(cat);
                out.addProperty("ok", ok);
                out.addProperty("message", ok ? "Categoría actualizada correctamente" : "No se pudo actualizar la categoría");
            } else if ("eliminar_categoria".equals(accion)) {
                int idCat = Integer.parseInt(req.getParameter("idCategoria"));
                boolean ok = InventarioRepository.eliminarCategoria(idCat);
                out.addProperty("ok", ok);
                out.addProperty("message", ok ? "Categoría eliminada" : "No se pudo eliminar la categoría");
            // ── Items (se mantienen para donaciones) ────────────────────────
            } else if ("registrar".equals(accion)) {
                InventarioItem item = construirDesdeRequest(req);
                item.setStockActual(0);
                item.setEstado("ACTIVO");
                int newId = InventarioRepository.registrar(item);
                boolean ok = newId > 0;
                out.addProperty("ok", ok);
                if (ok) out.addProperty("idItem", newId);
                out.addProperty("message", ok ? "Item registrado correctamente" : "No se pudo registrar el item");
            } else if ("actualizar".equals(accion)) {
                int idItem = Integer.parseInt(req.getParameter("idItem"));
                InventarioItem actual = InventarioRepository.obtenerPorId(idItem);
                boolean ok = false;
                if (actual != null) {
                    InventarioItem cambios = construirDesdeRequest(req);
                    actual.setNombre(cambios.getNombre());
                    actual.setCategoria(cambios.getCategoria());
                    actual.setUnidadMedida(cambios.getUnidadMedida());
                    actual.setStockMinimo(cambios.getStockMinimo());
                    actual.setObservacion(cambios.getObservacion());
                    ok = InventarioRepository.actualizar(actual);
                }
                out.addProperty("ok", ok);
                out.addProperty("message", ok ? "Item actualizado correctamente" : "No se pudo actualizar el item");
            } else if ("cambiar_estado".equals(accion)) {
                int idItem = Integer.parseInt(req.getParameter("idItem"));
                String estado = req.getParameter("estado");
                boolean ok = InventarioRepository.cambiarEstado(idItem, estado);
                out.addProperty("ok", ok);
                out.addProperty("message", ok ? "Estado actualizado" : "No se pudo actualizar el estado");
            } else if ("registrar_movimiento".equals(accion)) {
                int idItem = Integer.parseInt(req.getParameter("idItem"));
                String tipo = req.getParameter("tipoMovimiento");
                String motivo = req.getParameter("motivo");
                double cantidad = parseDouble(req.getParameter("cantidad"));
                String observacion = req.getParameter("observacion");
                Usuario usuario = (Usuario) session.getAttribute("usuarioLogeado");
                boolean ok = InventarioRepository.registrarMovimiento(idItem, tipo, motivo, cantidad, observacion, usuario.getIdUsuario());
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

    private CategoriaInventario construirCategoriaDesdeRequest(HttpServletRequest req) {
        CategoriaInventario cat = new CategoriaInventario();
        cat.setNombre(req.getParameter("nombre") != null ? req.getParameter("nombre").trim() : "");
        cat.setDescripcion(req.getParameter("descripcion") != null ? req.getParameter("descripcion").trim() : null);
        String color = req.getParameter("color");
        cat.setColor(color != null && !color.isEmpty() ? color : "#6366f1");
        String icono = req.getParameter("icono");
        cat.setIcono(icono != null && !icono.isEmpty() ? icono : "fa-box");
        return cat;
    }

    private InventarioItem construirDesdeRequest(HttpServletRequest req) {
        InventarioItem item = new InventarioItem();
        item.setNombre(req.getParameter("nombre") != null ? req.getParameter("nombre").trim() : null);
        item.setCategoria(req.getParameter("categoria") != null ? req.getParameter("categoria").trim() : null);
        item.setUnidadMedida(req.getParameter("unidadMedida") != null ? req.getParameter("unidadMedida").trim() : null);
        item.setStockMinimo(parseDouble(req.getParameter("stockMinimo")));
        item.setObservacion(req.getParameter("observacion") != null ? req.getParameter("observacion").trim() : null);
        return item;
    }

    private double parseDouble(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return 0;
        }
        return Double.parseDouble(valor);
    }
}
