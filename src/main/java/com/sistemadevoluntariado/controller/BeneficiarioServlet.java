package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.sistemadevoluntariado.dao.BeneficiarioDAO;
import com.sistemadevoluntariado.model.Beneficiario;
import com.sistemadevoluntariado.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/beneficiarios")
public class BeneficiarioServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private BeneficiarioDAO beneficiarioDAO = new BeneficiarioDAO();

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
            listarBeneficiarios(request, response);
        } else if ("obtener".equals(action)) {
            obtenerBeneficiario(request, response);
        } else {
            // Vista principal
            List<Beneficiario> beneficiarios = beneficiarioDAO.obtenerTodosBeneficiarios();
            request.setAttribute("beneficiarios", beneficiarios);
            request.setAttribute("usuario", usuario);
            request.getRequestDispatcher("/views/beneficiarios/listar.jsp").forward(request, response);
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
        System.out.println("► POST /beneficiarios  action=" + action);

        if ("crear".equals(action)) {
            crearBeneficiario(request, response, usuario);
        } else if ("editar".equals(action) || "actualizar".equals(action)) {
            actualizarBeneficiario(request, response);
        } else if ("cambiarEstado".equals(action)) {
            cambiarEstado(request, response);
        } else if ("eliminar".equals(action)) {
            eliminarBeneficiario(request, response);
        } else {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"message\": \"Acción no válida: " + action + "\"}");
        }
    }

    // ── CREAR ──────────────────────────────────────────────
    private void crearBeneficiario(HttpServletRequest request, HttpServletResponse response, Usuario usuario)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            String nombres            = request.getParameter("nombres");
            String apellidos          = request.getParameter("apellidos");
            String dni                = request.getParameter("dni");
            String fechaNacimiento    = request.getParameter("fechaNacimiento");
            String telefono           = request.getParameter("telefono");
            String direccion          = request.getParameter("direccion");
            String distrito           = request.getParameter("distrito");
            String tipoBeneficiario   = request.getParameter("tipoBeneficiario");
            String necesidadPrincipal = request.getParameter("necesidadPrincipal");
            String observaciones      = request.getParameter("observaciones");

            // Validaciones básicas
            if (nombres == null || nombres.trim().isEmpty()
                    || apellidos == null || apellidos.trim().isEmpty()
                    || dni == null || dni.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Nombres, apellidos y DNI son obligatorios\"}");
                return;
            }

            Beneficiario b = new Beneficiario(nombres, apellidos, dni, telefono, direccion,
                    distrito, tipoBeneficiario, necesidadPrincipal);
            b.setFechaNacimiento(fechaNacimiento);
            b.setObservaciones(observaciones);
            b.setIdUsuario(usuario.getIdUsuario());

            boolean resultado = beneficiarioDAO.crearBeneficiario(b);

            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Beneficiario registrado correctamente\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Error al registrar el beneficiario\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Error desconocido";
            response.getWriter().write("{\"success\": false, \"message\": \"Error interno: " + msg + "\"}");
        }
    }

    // ── LISTAR (JSON) ──────────────────────────────────────
    private void listarBeneficiarios(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<Beneficiario> beneficiarios = beneficiarioDAO.obtenerTodosBeneficiarios();
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(beneficiarios));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // ── OBTENER POR ID (JSON) ──────────────────────────────
    private void obtenerBeneficiario(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            Beneficiario beneficiario = beneficiarioDAO.obtenerBeneficiarioPorId(id);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(beneficiario));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // ── ACTUALIZAR ─────────────────────────────────────────
    private void actualizarBeneficiario(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            String idStr = request.getParameter("id");
            if (idStr == null || idStr.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"ID del beneficiario es obligatorio\"}");
                return;
            }

            int id = Integer.parseInt(idStr);
            Beneficiario b = new Beneficiario();
            b.setIdBeneficiario(id);
            b.setNombres(request.getParameter("nombres"));
            b.setApellidos(request.getParameter("apellidos"));
            b.setDni(request.getParameter("dni"));
            b.setFechaNacimiento(request.getParameter("fechaNacimiento"));
            b.setTelefono(request.getParameter("telefono"));
            b.setDireccion(request.getParameter("direccion"));
            b.setDistrito(request.getParameter("distrito"));
            b.setTipoBeneficiario(request.getParameter("tipoBeneficiario"));
            b.setNecesidadPrincipal(request.getParameter("necesidadPrincipal"));
            b.setObservaciones(request.getParameter("observaciones"));

            boolean resultado = beneficiarioDAO.actualizarBeneficiario(b);
            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Beneficiario actualizado correctamente\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Error al actualizar el beneficiario\"}");
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\": false, \"message\": \"ID inválido\"}");
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
            boolean resultado = beneficiarioDAO.cambiarEstado(id, estado);
            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Estado actualizado correctamente\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Error al cambiar el estado\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\": false, \"message\": \"Error: " + e.getMessage() + "\"}");
        }
    }

    // ── ELIMINAR ───────────────────────────────────────────
    private void eliminarBeneficiario(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            boolean resultado = beneficiarioDAO.eliminarBeneficiario(id);
            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Beneficiario eliminado correctamente\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Error al eliminar el beneficiario\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\": false, \"message\": \"Error: " + e.getMessage() + "\"}");
        }
    }
}
