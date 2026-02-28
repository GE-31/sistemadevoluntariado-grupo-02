package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.sistemadevoluntariado.entity.Actividad;
import com.sistemadevoluntariado.entity.Certificado;
import com.sistemadevoluntariado.entity.Usuario;
import com.sistemadevoluntariado.entity.Voluntario;
import com.sistemadevoluntariado.repository.ActividadRepository;
import com.sistemadevoluntariado.repository.AsistenciaRepository;
import com.sistemadevoluntariado.repository.CertificadoRepository;
import com.sistemadevoluntariado.repository.VoluntarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/certificados")
public class CertificadoController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private CertificadoRepository certificadoRepository;
    private VoluntarioRepository voluntarioRepository;
    private ActividadRepository actividadRepository;
    private AsistenciaRepository asistenciaRepository;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (certificadoRepository == null) certificadoRepository = new CertificadoRepository();
        if (voluntarioRepository == null) voluntarioRepository = new VoluntarioRepository();
        if (actividadRepository == null) actividadRepository = new ActividadRepository();
        if (asistenciaRepository == null) asistenciaRepository = new AsistenciaRepository();

        HttpSession session = request.getSession(false);
        Usuario usuario = session != null ? (Usuario) session.getAttribute("usuarioLogeado") : null;

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");

        if ("listar".equals(action)) {
            listarCertificadosJSON(request, response);
        } else if ("obtener".equals(action)) {
            obtenerCertificado(request, response);
        } else if ("verificar".equals(action)) {
            verificarCertificado(request, response);
        } else if ("voluntarios".equals(action)) {
            listarVoluntariosJSON(response);
        } else if ("actividades".equals(action)) {
            listarActividadesJSON(response);
        } else if ("horasVoluntario".equals(action)) {
            obtenerHorasVoluntarioActividad(request, response);
        } else {
            // Vista principal
            List<Certificado> certificados = certificadoRepository.obtenerTodosCertificados();
            List<Voluntario> voluntarios = voluntarioRepository.obtenerTodosVoluntarios();
            List<Actividad> actividades = actividadRepository.obtenerTodasActividades();

            request.setAttribute("certificados", certificados);
            request.setAttribute("voluntarios", voluntarios);
            request.setAttribute("actividades", actividades);
            request.setAttribute("usuario", usuario);
            request.getRequestDispatcher("/views/certificados/listar.html").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (certificadoRepository == null) certificadoRepository = new CertificadoRepository();
        if (voluntarioRepository == null) voluntarioRepository = new VoluntarioRepository();
        if (actividadRepository == null) actividadRepository = new ActividadRepository();

        HttpSession session = request.getSession(false);
        Usuario usuario = session != null ? (Usuario) session.getAttribute("usuarioLogeado") : null;

        if (usuario == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"message\": \"No autorizado\"}");
            return;
        }

        String action = request.getParameter("action");
        System.out.println("► POST /certificados  action=" + action);

        if ("crear".equals(action)) {
            crearCertificado(request, response, usuario);
        } else if ("anular".equals(action)) {
            anularCertificado(request, response);
        } else {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"message\": \"Acción no válida: " + action + "\"}");
        }
    }

    // ── CREAR CERTIFICADO ──────────────────────────────────
    private void crearCertificado(HttpServletRequest request, HttpServletResponse response, Usuario usuario)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            String idVoluntarioStr = request.getParameter("idVoluntario");
            String idActividadStr = request.getParameter("idActividad");
            String horasStr = request.getParameter("horasVoluntariado");
            String observaciones = request.getParameter("observaciones");

            // Validaciones
            if (idVoluntarioStr == null || idVoluntarioStr.trim().isEmpty()
                    || idActividadStr == null || idActividadStr.trim().isEmpty()
                    || horasStr == null || horasStr.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter()
                        .write("{\"success\": false, \"message\": \"Voluntario, actividad y horas son obligatorios\"}");
                return;
            }

            int idVoluntario = Integer.parseInt(idVoluntarioStr);
            int idActividad  = Integer.parseInt(idActividadStr);

            // Validar duplicado: no permitir dos certificados EMITIDO para el mismo voluntario+actividad
            if (certificadoRepository.existeCertificadoActivo(idVoluntario, idActividad)) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().write("{\"success\": false, \"message\": \"Este voluntario ya tiene un certificado emitido para esta actividad\"}");
                return;
            }

            Certificado certificado = new Certificado();
            certificado.setIdVoluntario(idVoluntario);
            certificado.setIdActividad(idActividad);
            certificado.setHorasVoluntariado(Integer.parseInt(horasStr));
            certificado.setObservaciones(observaciones);
            certificado.setIdUsuarioEmite(usuario.getIdUsuario());

            boolean resultado = certificadoRepository.crearCertificado(certificado);

            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Certificado emitido correctamente\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Error al emitir el certificado\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\": false, \"message\": \"Datos numéricos inválidos\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Error desconocido";
            response.getWriter().write("{\"success\": false, \"message\": \"Error interno: " + msg + "\"}");
        }
    }

    // ── LISTAR CERTIFICADOS (JSON) ─────────────────────────
    private void listarCertificadosJSON(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<Certificado> certificados = certificadoRepository.obtenerTodosCertificados();
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(certificados));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // ── OBTENER CERTIFICADO POR ID (JSON) ──────────────────
    private void obtenerCertificado(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            Certificado certificado = certificadoRepository.obtenerCertificadoPorId(id);
            response.setContentType("application/json;charset=UTF-8");
            if (certificado != null) {
                response.getWriter().write("{\"success\": true, \"certificado\": " + new Gson().toJson(certificado) + "}");
            } else {
                response.getWriter().write("{\"success\": false, \"message\": \"Certificado no encontrado\"}");
            }
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    // ── VERIFICAR CERTIFICADO POR CÓDIGO ───────────────────
    private void verificarCertificado(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String codigo = request.getParameter("codigo");
            Certificado certificado = certificadoRepository.obtenerCertificadoPorCodigo(codigo);
            response.setContentType("application/json;charset=UTF-8");
            if (certificado != null) {
                response.getWriter()
                        .write("{\"valid\": true, \"certificado\": " + new Gson().toJson(certificado) + "}");
            } else {
                response.getWriter().write("{\"valid\": false, \"message\": \"Certificado no encontrado\"}");
            }
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"valid\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    // ── ANULAR CERTIFICADO ─────────────────────────────────
    private void anularCertificado(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String motivo = request.getParameter("motivo");
            if (motivo == null || motivo.trim().isEmpty()) {
                motivo = "Sin motivo especificado";
            }
            boolean resultado = certificadoRepository.anularCertificado(id, motivo);
            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Certificado anulado correctamente\"}");
            } else {
                response.getWriter().write("{\"success\": false, \"message\": \"Error al anular el certificado\"}");
            }
        } catch (Exception e) {
            response.getWriter().write("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    // ── LISTAR VOLUNTARIOS CON ASISTENCIA (JSON) ─────────
    private void listarVoluntariosJSON(HttpServletResponse response) throws IOException {
        try {
            List<Voluntario> voluntarios = voluntarioRepository.obtenerVoluntariosConAsistencia();
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(voluntarios));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // ── LISTAR ACTIVIDADES (JSON) ──────────────────────────
    private void listarActividadesJSON(HttpServletResponse response) throws IOException {
        try {
            List<Actividad> actividades = actividadRepository.obtenerTodasActividades();
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(actividades));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    // ── OBTENER HORAS DE VOLUNTARIO EN ACTIVIDAD (JSON) ────
    private void obtenerHorasVoluntarioActividad(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            int idVoluntario = Integer.parseInt(request.getParameter("idVoluntario"));
            int idActividad = Integer.parseInt(request.getParameter("idActividad"));
            java.math.BigDecimal horas = asistenciaRepository.obtenerHorasVoluntarioActividad(idVoluntario, idActividad);
            response.getWriter().write("{\"success\": true, \"horas\": " + horas.toString() + "}");
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\": false, \"message\": \"Parámetros inválidos\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\": false, \"message\": \"Error interno\"}");
        }
    }}
