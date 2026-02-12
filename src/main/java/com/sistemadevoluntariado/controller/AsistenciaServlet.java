package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.sistemadevoluntariado.dao.ActividadDAO;
import com.sistemadevoluntariado.dao.AsistenciaDAO;
import com.sistemadevoluntariado.dao.VoluntarioDAO;
import com.sistemadevoluntariado.model.Actividad;
import com.sistemadevoluntariado.model.Asistencia;
import com.sistemadevoluntariado.model.Usuario;
import com.sistemadevoluntariado.model.Voluntario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/asistencias")
public class AsistenciaServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private AsistenciaDAO asistenciaDAO = new AsistenciaDAO();
    private ActividadDAO actividadDAO = new ActividadDAO();
    private VoluntarioDAO voluntarioDAO = new VoluntarioDAO();

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
            listarAsistenciasJson(request, response);
        } else if ("obtener".equals(action)) {
            obtenerAsistencia(request, response);
        } else if ("porActividad".equals(action)) {
            listarPorActividad(request, response);
        } else if ("porVoluntario".equals(action)) {
            listarPorVoluntario(request, response);
        } else {
            // Vista principal
            List<Asistencia> asistencias = asistenciaDAO.listarAsistencias();
            List<Actividad> actividades = actividadDAO.obtenerTodasActividades();
            List<Voluntario> voluntarios = voluntarioDAO.obtenerTodosVoluntarios();

            request.setAttribute("asistencias", asistencias);
            request.setAttribute("actividades", actividades);
            request.setAttribute("voluntarios", voluntarios);
            request.setAttribute("usuario", usuario);
            request.getRequestDispatcher("/views/asistencias/listar.jsp").forward(request, response);
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

        if ("registrar".equals(action)) {
            registrarAsistencia(request, response, usuario);
        } else if ("actualizar".equals(action)) {
            actualizarAsistencia(request, response);
        } else if ("eliminar".equals(action)) {
            eliminarAsistencia(request, response);
        } else {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"message\": \"Acción no válida: " + action + "\"}");
        }
    }

    private void registrarAsistencia(HttpServletRequest request, HttpServletResponse response, Usuario usuario)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        try {
            String idVoluntarioStr = request.getParameter("id_voluntario");
            String idActividadStr = request.getParameter("id_actividad");
            String fecha = request.getParameter("fecha");
            String horaEntrada = request.getParameter("hora_entrada");
            String horaSalida = request.getParameter("hora_salida");
            String estado = request.getParameter("estado");
            String observaciones = request.getParameter("observaciones");

            if (idVoluntarioStr == null || idVoluntarioStr.trim().isEmpty()
                    || idActividadStr == null || idActividadStr.trim().isEmpty()
                    || fecha == null || fecha.trim().isEmpty()
                    || estado == null || estado.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Voluntario, actividad, fecha y estado son obligatorios\"}");
                return;
            }

            Asistencia asistencia = new Asistencia();
            asistencia.setIdVoluntario(Integer.parseInt(idVoluntarioStr));
            asistencia.setIdActividad(Integer.parseInt(idActividadStr));
            asistencia.setFecha(fecha);
            asistencia.setHoraEntrada(horaEntrada);
            asistencia.setHoraSalida(horaSalida);
            asistencia.setEstado(estado);
            asistencia.setObservaciones(observaciones);
            asistencia.setIdUsuarioRegistro(usuario.getIdUsuario());

            boolean resultado = asistenciaDAO.registrarAsistencia(asistencia);

            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Asistencia registrada correctamente\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Error al registrar asistencia. Puede que ya exista un registro para ese voluntario en esa actividad y fecha.\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\": false, \"message\": \"ID inválido\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Error desconocido";
            response.getWriter().write("{\"success\": false, \"message\": \"Error interno: " + errorMsg + "\"}");
        }
    }

    private void actualizarAsistencia(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        try {
            String idStr = request.getParameter("id");
            if (idStr == null || idStr.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"ID de asistencia es obligatorio\"}");
                return;
            }

            Asistencia asistencia = new Asistencia();
            asistencia.setIdAsistencia(Integer.parseInt(idStr));
            asistencia.setHoraEntrada(request.getParameter("hora_entrada"));
            asistencia.setHoraSalida(request.getParameter("hora_salida"));
            asistencia.setEstado(request.getParameter("estado"));
            asistencia.setObservaciones(request.getParameter("observaciones"));

            boolean resultado = asistenciaDAO.actualizarAsistencia(asistencia);

            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Asistencia actualizada correctamente\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Error al actualizar asistencia\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Error desconocido";
            response.getWriter().write("{\"success\": false, \"message\": \"Error: " + errorMsg + "\"}");
        }
    }

    private void eliminarAsistencia(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        try {
            int id = Integer.parseInt(request.getParameter("id"));
            boolean resultado = asistenciaDAO.eliminarAsistencia(id);

            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Asistencia eliminada correctamente\"}");
            } else {
                response.getWriter().write("{\"success\": false, \"message\": \"Error al eliminar asistencia\"}");
            }
        } catch (Exception e) {
            response.getWriter().write("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    private void listarAsistenciasJson(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            List<Asistencia> asistencias = asistenciaDAO.listarAsistencias();
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(asistencias));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void obtenerAsistencia(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            Asistencia asistencia = asistenciaDAO.obtenerPorId(id);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(asistencia));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void listarPorActividad(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            int idActividad = Integer.parseInt(request.getParameter("id_actividad"));
            List<Asistencia> lista = asistenciaDAO.listarPorActividad(idActividad);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(lista));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void listarPorVoluntario(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            int idVoluntario = Integer.parseInt(request.getParameter("id_voluntario"));
            List<Asistencia> lista = asistenciaDAO.listarPorVoluntario(idVoluntario);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(lista));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
