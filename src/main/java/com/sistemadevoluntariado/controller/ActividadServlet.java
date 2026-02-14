package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.sistemadevoluntariado.dao.ActividadDAO;
import com.sistemadevoluntariado.model.Actividad;
import com.sistemadevoluntariado.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/actividades")
public class ActividadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private ActividadDAO actividadDAO = new ActividadDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Usuario usuario = session != null ? (Usuario) session.getAttribute("usuarioLogeado") : null;

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");

        if ("listar".equals(action)) {
            listarActividades(request, response);
        } else if ("obtener".equals(action)) {
            obtenerActividad(request, response);
        } else {
            // Vista principal
            List<Actividad> actividades = actividadDAO.obtenerTodasActividades();
            request.setAttribute("actividades", actividades);
            request.setAttribute("usuario", usuario);
            request.getRequestDispatcher("/views/actividades/listar.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Usuario usuario = session != null ? (Usuario) session.getAttribute("usuarioLogeado") : null;

        if (usuario == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"message\": \"No autorizado\"}");
            return;
        }

        String action = request.getParameter("action");
        System.out.println("► POST /actividades  action=" + action);

        if ("crear".equals(action)) {
            crearActividad(request, response, usuario);
        } else if ("editar".equals(action) || "actualizar".equals(action)) {
            actualizarActividad(request, response);
        } else if ("cambiarEstado".equals(action)) {
            cambiarEstado(request, response);
        } else if ("eliminar".equals(action)) {
            eliminarActividad(request, response);
        } else {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"message\": \"Acción no válida: " + action + "\"}");
        }
    }

    // ── CREAR ──────────────────────────────────────────────
    private void crearActividad(HttpServletRequest request, HttpServletResponse response, Usuario usuario)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            String nombre = request.getParameter("nombre");
            String descripcion = request.getParameter("descripcion");
            String fechaInicio = request.getParameter("fechaInicio");
            String fechaFin = request.getParameter("fechaFin");
            String ubicacion = request.getParameter("ubicacion");
            String cupoStr = request.getParameter("cupoMaximo");

            // Validaciones básicas
            if (nombre == null || nombre.trim().isEmpty()
                    || fechaInicio == null || fechaInicio.trim().isEmpty()
                    || ubicacion == null || ubicacion.trim().isEmpty()
                    || cupoStr == null || cupoStr.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(
                        "{\"success\": false, \"message\": \"Los campos nombre, fecha inicio, ubicación y cupo son obligatorios\"}");
                return;
            }

            int cupoMaximo = Integer.parseInt(cupoStr);

            Actividad actividad = new Actividad(nombre, descripcion, fechaInicio, fechaFin, ubicacion, cupoMaximo);
            actividad.setIdUsuario(usuario.getIdUsuario());

            boolean resultado = actividadDAO.crearActividad(actividad);

            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Actividad creada correctamente\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Error al crear la actividad\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter()
                    .write("{\"success\": false, \"message\": \"El cupo máximo debe ser un número válido\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Error desconocido";
            response.getWriter().write("{\"success\": false, \"message\": \"Error interno: " + msg + "\"}");
        }
    }

    // ── LISTAR (JSON) ──────────────────────────────────────
    private void listarActividades(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<Actividad> actividades = actividadDAO.obtenerTodasActividades();
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(actividades));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // ── OBTENER POR ID (JSON) ──────────────────────────────
    private void obtenerActividad(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            Actividad actividad = actividadDAO.obtenerActividadPorId(id);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(actividad));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // ── ACTUALIZAR ─────────────────────────────────────────
    private void actualizarActividad(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            String idStr = request.getParameter("id");
            if (idStr == null || idStr.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"ID de la actividad es obligatorio\"}");
                return;
            }

            int id = Integer.parseInt(idStr);
            Actividad actividad = new Actividad();
            actividad.setIdActividad(id);
            actividad.setNombre(request.getParameter("nombre"));
            actividad.setDescripcion(request.getParameter("descripcion"));
            actividad.setFechaInicio(request.getParameter("fechaInicio"));
            actividad.setFechaFin(request.getParameter("fechaFin"));
            actividad.setUbicacion(request.getParameter("ubicacion"));
            actividad.setCupoMaximo(Integer.parseInt(request.getParameter("cupoMaximo")));

            boolean resultado = actividadDAO.actualizarActividad(actividad);
            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Actividad actualizada correctamente\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Error al actualizar la actividad\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\": false, \"message\": \"ID o cupo inválido\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Error desconocido";
            response.getWriter().write("{\"success\": false, \"message\": \"Error: " + msg + "\"}");
        }
    }

    // ── CAMBIAR ESTADO ─────────────────────────────────────
    private void cambiarEstado(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String estado = request.getParameter("estado");
            boolean resultado = actividadDAO.cambiarEstado(id, estado);
            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Estado actualizado correctamente\"}");
            } else {
                response.getWriter().write("{\"success\": false, \"message\": \"Error al cambiar estado\"}");
            }
        } catch (Exception e) {
            response.getWriter().write("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    // ── ELIMINAR ───────────────────────────────────────────
    private void eliminarActividad(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            boolean resultado = actividadDAO.eliminarActividad(id);
            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Actividad eliminada correctamente\"}");
            } else {
                response.getWriter().write("{\"success\": false, \"message\": \"Error al eliminar la actividad\"}");
            }
        } catch (Exception e) {
            response.getWriter().write("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }
}
