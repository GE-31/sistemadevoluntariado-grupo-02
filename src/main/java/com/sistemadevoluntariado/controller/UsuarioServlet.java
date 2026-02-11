package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sistemadevoluntariado.dao.UsuarioDAO;
import com.sistemadevoluntariado.dao.VoluntarioDAO;
import com.sistemadevoluntariado.dao.RolSistemaDAO;
import com.sistemadevoluntariado.model.Usuario;
import com.sistemadevoluntariado.model.Voluntario;
import com.sistemadevoluntariado.model.RolSistema;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/usuarios")
public class UsuarioServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private UsuarioDAO usuarioDAO = new UsuarioDAO();
    private VoluntarioDAO voluntarioDAO = new VoluntarioDAO();
    private RolSistemaDAO rolSistemaDAO = new RolSistemaDAO();
    private Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Verificar si el usuario está logueado
        if (session == null || session.getAttribute("usuarioLogeado") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Usuario usuarioLogeado = (Usuario) session.getAttribute("usuarioLogeado");
        request.setAttribute("usuario", usuarioLogeado);

        String action = request.getParameter("action");

        if ("obtener".equals(action)) {
            // Obtener un usuario específico por ID
            int id = Integer.parseInt(request.getParameter("id"));
            Usuario usuario = usuarioDAO.obtenerUsuarioPorId(id);

            if (usuario != null) {
                response.setContentType("application/json");
                response.getWriter().write(gson.toJson(usuario));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            // Obtener todos los usuarios
            List<Usuario> usuarios = usuarioDAO.obtenerTodosUsuarios();
            request.setAttribute("usuarios", usuarios);

            // Obtener todos los voluntarios para el dropdown
            List<Voluntario> voluntarios = voluntarioDAO.obtenerTodosVoluntarios();
            request.setAttribute("voluntarios", voluntarios);

            // Obtener todos los roles del sistema para el dropdown
            List<RolSistema> roles = rolSistemaDAO.obtenerTodosRoles();
            request.setAttribute("roles", roles);

            request.setAttribute("page", "usuarios");

            request.getRequestDispatcher("/views/usuarios/usuario.jsp")
                    .forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Verificar si el usuario está logueado
        if (session == null || session.getAttribute("usuarioLogeado") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        response.setContentType("application/json");

        String action = request.getParameter("action");
        JsonObject responseJson = new JsonObject();

        try {
            if ("crear".equals(action)) {
                crearUsuario(request, response, responseJson);
            } else if ("editar".equals(action)) {
                editarUsuario(request, response, responseJson);
            } else if ("eliminar".equals(action)) {
                eliminarUsuario(request, response, responseJson);
            } else if ("cambiar_estado".equals(action)) {
                cambiarEstado(request, response, responseJson);
            } else {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Acción no válida");
            }
        } catch (Exception e) {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }

        response.getWriter().write(responseJson.toString());
    }

    private void crearUsuario(HttpServletRequest request, HttpServletResponse response, JsonObject responseJson) {
        String voluntarioIdStr = request.getParameter("voluntarioId");
        String username = request.getParameter("username");
        String rolSistemaIdStr = request.getParameter("rolSistema");
        String password = request.getParameter("password");

        // Validaciones
        if (voluntarioIdStr == null || voluntarioIdStr.trim().isEmpty()) {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "Debe seleccionar un voluntario");
            return;
        }

        if (username == null || username.trim().isEmpty()) {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "El username es requerido");
            return;
        }

        if (rolSistemaIdStr == null || rolSistemaIdStr.trim().isEmpty()) {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "Debe seleccionar un rol del sistema");
            return;
        }

        if (password == null || password.length() < 6) {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "La contraseña debe tener al menos 6 caracteres");
            return;
        }

        try {
            int voluntarioId = Integer.parseInt(voluntarioIdStr);
            int rolSistemaId = Integer.parseInt(rolSistemaIdStr);

            // Llamar al DAO para crear el usuario
            boolean creado = usuarioDAO.registrarUsuarioConVoluntario(voluntarioId, rolSistemaId, username, password);

            if (creado) {
                responseJson.addProperty("success", true);
                responseJson.addProperty("message", "Usuario creado exitosamente");
            } else {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Error al crear el usuario");
            }
        } catch (NumberFormatException e) {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "IDs inválidos");
        }
    }

    private void editarUsuario(HttpServletRequest request, HttpServletResponse response, JsonObject responseJson) {
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String voluntarioIdStr = request.getParameter("voluntarioId");
            String username = request.getParameter("username");
            String rolSistemaIdStr = request.getParameter("rolSistema");

            // Validaciones
            if (voluntarioIdStr == null || voluntarioIdStr.trim().isEmpty()) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Debe seleccionar un voluntario");
                return;
            }

            if (username == null || username.trim().isEmpty()) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "El username es requerido");
                return;
            }

            if (rolSistemaIdStr == null || rolSistemaIdStr.trim().isEmpty()) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Debe seleccionar un rol del sistema");
                return;
            }

            int voluntarioId = Integer.parseInt(voluntarioIdStr);
            int rolSistemaId = Integer.parseInt(rolSistemaIdStr);

            // TODO: Actualizar método en UsuarioDAO para editar con voluntarioId y
            // rolSistemaId
            responseJson.addProperty("success", true);
            responseJson.addProperty("message", "Usuario actualizado exitosamente (pendiente implementación en DAO)");
        } catch (Exception e) {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "Error al editar: " + e.getMessage());
        }
    }

    private void cambiarEstado(HttpServletRequest request, HttpServletResponse response, JsonObject responseJson) {
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String nuevoEstado = request.getParameter("estado");

            // Usar procedimiento almacenado del DAO
            boolean actualizado = usuarioDAO.cambiarEstadoUsuario(id, nuevoEstado);

            if (actualizado) {
                responseJson.addProperty("success", true);
                responseJson.addProperty("message", "Estado actualizado a: " + nuevoEstado);
            } else {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Error al cambiar el estado");
            }
        } catch (Exception e) {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "Error al cambiar estado: " + e.getMessage());
        }
    }

    private void eliminarUsuario(HttpServletRequest request, HttpServletResponse response, JsonObject responseJson) {
        try {
            int id = Integer.parseInt(request.getParameter("id"));

            // Usar procedimiento almacenado del DAO
            boolean eliminado = usuarioDAO.eliminarUsuario(id);

            if (eliminado) {
                responseJson.addProperty("success", true);
                responseJson.addProperty("message", "Usuario eliminado exitosamente");
            } else {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Error al eliminar el usuario");
            }
        } catch (Exception e) {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "Error al eliminar: " + e.getMessage());
        }
    }
}
