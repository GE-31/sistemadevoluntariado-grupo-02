package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.sistemadevoluntariado.dao.ActividadDAO;
import com.sistemadevoluntariado.dao.CertificadoDAO;
import com.sistemadevoluntariado.dao.VoluntarioDAO;
import com.sistemadevoluntariado.model.Actividad;
import com.sistemadevoluntariado.model.Certificado;
import com.sistemadevoluntariado.model.Usuario;
import com.sistemadevoluntariado.model.Voluntario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/certificados")
public class CertificadoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private CertificadoDAO certificadoDAO = new CertificadoDAO();
    private VoluntarioDAO voluntarioDAO = new VoluntarioDAO();
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
            listarCertificadosJSON(request, response);
        } else if ("obtener".equals(action)) {
            obtenerCertificado(request, response);
        } else if ("verificar".equals(action)) {
            verificarCertificado(request, response);
        } else if ("voluntarios".equals(action)) {
            listarVoluntariosJSON(response);
        } else if ("actividades".equals(action)) {
            listarActividadesJSON(response);
        } else {
            // Vista principal
            List<Certificado> certificados = certificadoDAO.obtenerTodosCertificados();
            List<Voluntario> voluntarios = voluntarioDAO.obtenerTodosVoluntarios();
            List<Actividad> actividades = actividadDAO.obtenerTodasActividades();

            request.setAttribute("certificados", certificados);
            request.setAttribute("voluntarios", voluntarios);
            request.setAttribute("actividades", actividades);
            request.setAttribute("usuario", usuario);
            request.getRequestDispatcher("/views/certificados/listar.jsp").forward(request, response);
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

            Certificado certificado = new Certificado();
            certificado.setIdVoluntario(Integer.parseInt(idVoluntarioStr));
            certificado.setIdActividad(Integer.parseInt(idActividadStr));
            certificado.setHorasVoluntariado(Integer.parseInt(horasStr));
            certificado.setObservaciones(observaciones);
            certificado.setIdUsuarioEmite(usuario.getIdUsuario());

            boolean resultado = certificadoDAO.crearCertificado(certificado);

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
            List<Certificado> certificados = certificadoDAO.obtenerTodosCertificados();
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
            Certificado certificado = certificadoDAO.obtenerCertificadoPorId(id);
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
            Certificado certificado = certificadoDAO.obtenerCertificadoPorCodigo(codigo);
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
            boolean resultado = certificadoDAO.anularCertificado(id, motivo);
            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Certificado anulado correctamente\"}");
            } else {
                response.getWriter().write("{\"success\": false, \"message\": \"Error al anular el certificado\"}");
            }
        } catch (Exception e) {
            response.getWriter().write("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    // ── LISTAR VOLUNTARIOS (JSON) ──────────────────────────
    private void listarVoluntariosJSON(HttpServletResponse response) throws IOException {
        try {
            List<Voluntario> voluntarios = voluntarioDAO.obtenerTodosVoluntarios();
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
            List<Actividad> actividades = actividadDAO.obtenerTodasActividades();
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(actividades));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
