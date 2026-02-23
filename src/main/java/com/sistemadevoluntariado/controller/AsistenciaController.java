package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.google.gson.Gson;
import com.sistemadevoluntariado.entity.Actividad;
import com.sistemadevoluntariado.entity.Asistencia;
import com.sistemadevoluntariado.entity.Usuario;
import com.sistemadevoluntariado.entity.Voluntario;
import com.sistemadevoluntariado.repository.ActividadRepository;
import com.sistemadevoluntariado.repository.AsistenciaRepository;
import com.sistemadevoluntariado.repository.VoluntarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/asistencias")
public class AsistenciaController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private AsistenciaRepository asistenciaRepository;
    private ActividadRepository actividadRepository;
    private VoluntarioRepository voluntarioRepository;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (asistenciaRepository == null) asistenciaRepository = new AsistenciaRepository();
        if (actividadRepository == null) actividadRepository = new ActividadRepository();
        if (voluntarioRepository == null) voluntarioRepository = new VoluntarioRepository();

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
            List<Asistencia> asistencias = asistenciaRepository.listarAsistencias();
            List<Actividad> actividades = actividadRepository.obtenerTodasActividades();
            List<Voluntario> voluntarios = voluntarioRepository.obtenerTodosVoluntarios();

            request.setAttribute("asistencias", asistencias);
            request.setAttribute("actividades", actividades);
            request.setAttribute("voluntarios", voluntarios);
            request.setAttribute("usuario", usuario);
            request.getRequestDispatcher("/views/asistencias/listar.html").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (asistenciaRepository == null) asistenciaRepository = new AsistenciaRepository();
        if (actividadRepository == null) actividadRepository = new ActividadRepository();
        if (voluntarioRepository == null) voluntarioRepository = new VoluntarioRepository();

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

            // Normalizar cadenas vacías a null
            if (horaEntrada != null && horaEntrada.trim().isEmpty()) horaEntrada = null;
            if (horaSalida != null && horaSalida.trim().isEmpty()) horaSalida = null;
            if (observaciones != null && observaciones.trim().isEmpty()) observaciones = null;

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
            asistencia.setHorasTotales(calcularHoras(horaEntrada, horaSalida));

            boolean resultado = asistenciaRepository.registrarAsistencia(asistencia);

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
            String horaEnt = request.getParameter("hora_entrada");
            String horaSal = request.getParameter("hora_salida");
            asistencia.setHoraEntrada(horaEnt);
            asistencia.setHoraSalida(horaSal);
            asistencia.setEstado(request.getParameter("estado"));
            asistencia.setObservaciones(request.getParameter("observaciones"));
            asistencia.setHorasTotales(calcularHoras(horaEnt, horaSal));

            boolean resultado = asistenciaRepository.actualizarAsistencia(asistencia);

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
            boolean resultado = asistenciaRepository.eliminarAsistencia(id);

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
            List<Asistencia> asistencias = asistenciaRepository.listarAsistencias();
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
            Asistencia asistencia = asistenciaRepository.obtenerPorId(id);
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
            List<Asistencia> lista = asistenciaRepository.listarPorActividad(idActividad);
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
            List<Asistencia> lista = asistenciaRepository.listarPorVoluntario(idVoluntario);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(lista));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // Calcula la diferencia en horas entre hora_entrada y hora_salida (formato HH:mm o HH:mm:ss)
    private BigDecimal calcularHoras(String horaEntrada, String horaSalida) {
        try {
            if (horaEntrada == null || horaEntrada.trim().isEmpty()
                    || horaSalida == null || horaSalida.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }
            LocalTime entrada = LocalTime.parse(horaEntrada.length() == 5 ? horaEntrada + ":00" : horaEntrada);
            LocalTime salida  = LocalTime.parse(horaSalida.length()  == 5 ? horaSalida  + ":00" : horaSalida);
            long minutos = ChronoUnit.MINUTES.between(entrada, salida);
            if (minutos <= 0) return BigDecimal.ZERO;
            return new BigDecimal(minutos).divide(new BigDecimal(60), 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
