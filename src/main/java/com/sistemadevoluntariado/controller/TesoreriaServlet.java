package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.sistemadevoluntariado.dao.TesoreriaDAO;
import com.sistemadevoluntariado.model.MovimientoFinanciero;
import com.sistemadevoluntariado.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/tesoreria")
public class TesoreriaServlet extends HttpServlet {

    private final TesoreriaDAO dao = new TesoreriaDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String accion = req.getParameter("accion");

        if (accion != null) {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            switch (accion) {
                case "listar":
                    resp.getWriter().write(gson.toJson(dao.listar()));
                    return;

                case "obtener":
                    int id = Integer.parseInt(req.getParameter("id"));
                    MovimientoFinanciero m = dao.obtenerPorId(id);
                    resp.getWriter().write(gson.toJson(m));
                    return;

                case "balance":
                    Map<String, Double> balance = dao.obtenerBalance();
                    resp.getWriter().write(gson.toJson(balance));
                    return;

                case "filtrar":
                    String tipo = req.getParameter("tipo");
                    String categoria = req.getParameter("categoria");
                    String fechaIni = req.getParameter("fechaIni");
                    String fechaFin = req.getParameter("fechaFin");
                    List<MovimientoFinanciero> filtrados = dao.filtrar(tipo, categoria, fechaIni, fechaFin);
                    resp.getWriter().write(gson.toJson(filtrados));
                    return;

                case "resumenCategoria":
                    resp.getWriter().write(gson.toJson(dao.resumenPorCategoria()));
                    return;

                case "resumenMensual":
                    resp.getWriter().write(gson.toJson(dao.resumenMensual()));
                    return;

                case "eliminar":
                    int idEliminar = Integer.parseInt(req.getParameter("id"));
                    boolean eliminado = dao.eliminar(idEliminar);
                    resp.getWriter().write("{\"ok\":" + eliminado + "}");
                    return;

                default:
                    break;
            }
        }

        // Vista principal
        req.setAttribute("page", "tesoreria");
        req.setAttribute("movimientos", dao.listar());
        req.getRequestDispatcher("/views/tesoreria/tesoreria.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Usuario user = (Usuario) req.getSession().getAttribute("usuarioLogeado");

        String accion = req.getParameter("accion");
        if (accion == null) accion = "registrar";

        MovimientoFinanciero m = new MovimientoFinanciero();
        m.setTipo(req.getParameter("tipo"));
        m.setMonto(Double.parseDouble(req.getParameter("monto")));
        m.setDescripcion(req.getParameter("descripcion"));
        m.setCategoria(req.getParameter("categoria"));
        m.setComprobante(req.getParameter("comprobante"));
        m.setFechaMovimiento(req.getParameter("fechaMovimiento"));

        String idActStr = req.getParameter("idActividad");
        m.setIdActividad(idActStr != null && !idActStr.isEmpty() ? Integer.parseInt(idActStr) : 0);
        m.setIdUsuario(user.getIdUsuario());

        boolean ok;

        if ("actualizar".equals(accion)) {
            m.setIdMovimiento(Integer.parseInt(req.getParameter("idMovimiento")));
            ok = dao.actualizar(m);
        } else {
            ok = dao.registrar(m);
        }

        resp.getWriter().write("{\"ok\":" + ok + "}");
    }
}
