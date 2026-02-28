package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.sistemadevoluntariado.entity.Usuario;
import com.sistemadevoluntariado.entity.Voluntario;
import com.sistemadevoluntariado.repository.VoluntarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/voluntarios")
public class VoluntarioController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private VoluntarioRepository VoluntarioRepository;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (VoluntarioRepository == null) VoluntarioRepository = new VoluntarioRepository();
        HttpSession session = request.getSession(false);
        Usuario usuario = session != null ? (Usuario) session.getAttribute("usuarioLogeado") : null;

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");

        if ("listar".equals(action)) {
            listarVoluntarios(request, response);
        } else if ("obtener".equals(action)) {
            obtenerVoluntario(request, response);
        } else {
            // Vista principal de voluntarios
            List<Voluntario> voluntarios = VoluntarioRepository.obtenerTodosVoluntarios();
            request.setAttribute("voluntarios", voluntarios);
            request.setAttribute("usuario", usuario);
            request.getRequestDispatcher("/views/voluntarios/listar.html").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (VoluntarioRepository == null) VoluntarioRepository = new VoluntarioRepository();

        HttpSession session = request.getSession(false);
        Usuario usuario = session != null ? (Usuario) session.getAttribute("usuarioLogeado") : null;

        if (usuario == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"message\": \"No autorizado\"}");
            return;
        }

        String action = request.getParameter("action");
        System.out.println("► POST /voluntarios");
        System.out.println("  - action: " + action);
        System.out.println("  - nombres: " + request.getParameter("nombres"));
        System.out.println("  - apellidos: " + request.getParameter("apellidos"));
        System.out.println("  - Todos los parámetros: " + request.getParameterMap().keySet());

        if ("crear".equals(action)) {
            crearVoluntario(request, response);
        } else if ("editar".equals(action) || "actualizar".equals(action)) {
            actualizarVoluntario(request, response);
        } else if ("cambiarEstado".equals(action)) {
            cambiarEstado(request, response);
        } else if ("eliminar".equals(action)) {
            eliminarVoluntario(request, response);
        } else {
            System.out.println("✗ Acción no reconocida: " + action);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"message\": \"Acción no válida: " + action + "\"}");
        }
    }

    private void crearVoluntario(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        try {
            String nombres = request.getParameter("nombres");
            String apellidos = request.getParameter("apellidos");
            String dni = request.getParameter("dni");
            String correo = request.getParameter("correo");
            String telefono = request.getParameter("telefono");
            String carrera = request.getParameter("carrera");
            String cargo = request.getParameter("cargo");
            if (cargo == null || cargo.trim().isEmpty()) cargo = "Voluntario";

            System.out.println("► Intentando crear voluntario: " + nombres + " " + apellidos);

            // Validar datos no nulos
            if (nombres == null || nombres.trim().isEmpty()
                    || apellidos == null || apellidos.trim().isEmpty()
                    || dni == null || dni.trim().isEmpty()
                    || correo == null || correo.trim().isEmpty()
                    || telefono == null || telefono.trim().isEmpty()
                    || carrera == null || carrera.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Todos los campos son obligatorios\"}");
                return;
            }

            HttpSession sesion = request.getSession(false);
            if (sesion == null || sesion.getAttribute("usuarioLogeado") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"success\": false, \"message\": \"Sesión expirada. Por favor inicia sesión nuevamente\"}");
                return;
            }

            Voluntario voluntario = new Voluntario(nombres, apellidos, dni, correo, telefono, carrera);
            // null → sin cuenta de sistema aún; se vinculará al crear usuario
            voluntario.setIdUsuario(null);
            voluntario.setCargo(cargo);
            // Solo "Voluntario" no tiene acceso al sistema
            voluntario.setAccesoSistema(!cargo.equalsIgnoreCase("Voluntario"));

            boolean resultado = VoluntarioRepository.crearVoluntario(voluntario);

            if (resultado) {
                System.out.println("✓ Voluntario creado exitosamente");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("{\"success\": true, \"message\": \"Voluntario creado correctamente\"}");
            } else {
                System.out.println("✗ No se pudo crear el voluntario - DAO retornó false");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Error al crear voluntario. Verifica los datos y que no exista un registro duplicado\"}");
            }
        } catch (Exception e) {
            System.out.println("✗ Excepción en crearVoluntario: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Error desconocido";
            response.getWriter().write("{\"success\": false, \"message\": \"Error interno del servidor: " + errorMsg + "\"}");
        }
    }

    private void listarVoluntarios(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            List<Voluntario> voluntarios = VoluntarioRepository.obtenerTodosVoluntarios();
            response.setContentType("application/json;charset=UTF-8");
            Gson gson = new Gson();
            response.getWriter().write(gson.toJson(voluntarios));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void obtenerVoluntario(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            int idVoluntario = Integer.parseInt(request.getParameter("id"));
            Voluntario voluntario = VoluntarioRepository.obtenerVoluntarioPorId(idVoluntario);
            response.setContentType("application/json;charset=UTF-8");
            Gson gson = new Gson();
            response.getWriter().write(gson.toJson(voluntario));
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void actualizarVoluntario(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        try {
            String idStr = request.getParameter("id");
            if (idStr == null || idStr.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"ID del voluntario es obligatorio\"}");
                return;
            }

            int id = Integer.parseInt(idStr);
            String nombres = request.getParameter("nombres");
            String apellidos = request.getParameter("apellidos");
            String dni = request.getParameter("dni");
            String correo = request.getParameter("correo");
            String telefono = request.getParameter("telefono");
            String carrera = request.getParameter("carrera");
            String cargo = request.getParameter("cargo");
            if (cargo == null || cargo.trim().isEmpty()) cargo = "Voluntario";

            System.out.println("► Actualizando voluntario ID: " + id);

            Voluntario voluntario = VoluntarioRepository.obtenerVoluntarioPorId(id);
            if (voluntario == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"success\": false, \"message\": \"Voluntario no encontrado\"}");
                return;
            }
            voluntario.setNombres(nombres);
            voluntario.setApellidos(apellidos);
            voluntario.setDni(dni);
            voluntario.setCorreo(correo);
            voluntario.setTelefono(telefono);
            voluntario.setCarrera(carrera);
            voluntario.setCargo(cargo);
            voluntario.setAccesoSistema(!cargo.equalsIgnoreCase("Voluntario"));
            // idUsuario se preserva del objeto existente

            boolean resultado = VoluntarioRepository.actualizarVoluntario(voluntario);

            if (resultado) {
                System.out.println("✓ Voluntario actualizado correctamente ID: " + id);
                response.getWriter().write("{\"success\": true, \"message\": \"Voluntario actualizado correctamente\"}");
            } else {
                System.out.println("✗ Error al actualizar voluntario ID: " + id);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\": false, \"message\": \"Error al actualizar voluntario\"}");
            }
        } catch (NumberFormatException e) {
            System.out.println("✗ Error: ID no es un número válido - " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\": false, \"message\": \"ID inválido\"}");
        } catch (Exception e) {
            System.out.println("✗ Excepción en actualizarVoluntario: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Error desconocido";
            response.getWriter().write("{\"success\": false, \"message\": \"Error: " + errorMsg + "\"}");
        }
    }

    private void cambiarEstado(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String estado = request.getParameter("estado");

            boolean resultado = VoluntarioRepository.cambiarEstado(id, estado);

            response.setContentType("application/json;charset=UTF-8");
            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Estado actualizado correctamente\"}");
            } else {
                response.getWriter().write("{\"success\": false, \"message\": \"Error al cambiar estado\"}");
            }
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    private void eliminarVoluntario(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            boolean resultado = VoluntarioRepository.eliminarVoluntario(id);

            response.setContentType("application/json;charset=UTF-8");
            if (resultado) {
                response.getWriter().write("{\"success\": true, \"message\": \"Voluntario eliminado correctamente\"}");
            } else {
                response.getWriter().write("{\"success\": false, \"message\": \"Error al eliminar voluntario\"}");
            }
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\": false, \"message\": \"" + e.getMessage() + "\"}");
        }
    }
}
