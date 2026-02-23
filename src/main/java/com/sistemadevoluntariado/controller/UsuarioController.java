package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sistemadevoluntariado.repository.PermisoRepository;
import com.sistemadevoluntariado.repository.RolSistemaRepository;
import com.sistemadevoluntariado.repository.UsuarioRepository;
import com.sistemadevoluntariado.repository.VoluntarioRepository;
import com.sistemadevoluntariado.entity.Permiso;
import com.sistemadevoluntariado.entity.RolSistema;
import com.sistemadevoluntariado.entity.Usuario;
import com.sistemadevoluntariado.entity.Voluntario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/usuarios")
public class UsuarioController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private UsuarioRepository UsuarioRepository;
    private VoluntarioRepository VoluntarioRepository;
    private RolSistemaRepository RolSistemaRepository;
    private PermisoRepository PermisoRepository;
    private Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (UsuarioRepository == null) UsuarioRepository = new UsuarioRepository();
        if (VoluntarioRepository == null) VoluntarioRepository = new VoluntarioRepository();
        if (RolSistemaRepository == null) RolSistemaRepository = new RolSistemaRepository();
        if (PermisoRepository == null) PermisoRepository = new PermisoRepository();

        HttpSession session = request.getSession(false);

        // Verificar si el usuario está logueado
        if (session == null || session.getAttribute("usuarioLogeado") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Usuario usuarioLogeado = (Usuario) session.getAttribute("usuarioLogeado");
        request.setAttribute("usuario", usuarioLogeado);

        String action = request.getParameter("action");

        if ("voluntarios".equals(action)) {
            // Retorna JSON con voluntarios disponibles (acceso_sistema=true, sin usuario asignado)
            List<Voluntario> disponibles = VoluntarioRepository.obtenerVoluntariosConAcceso();
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(gson.toJson(disponibles));
            return;
        } else if ("obtener".equals(action)) {
            // Obtener un usuario específico por ID
            int id = Integer.parseInt(request.getParameter("id"));
            Usuario usuario = UsuarioRepository.obtenerUsuarioPorId(id);

            if (usuario != null) {
                // Incluir permisos asignados al usuario
                List<Integer> idsPermisos = PermisoRepository.obtenerPermisosDeUsuario(id);
                JsonObject obj = new JsonObject();
                obj.addProperty("idUsuario", usuario.getIdUsuario());
                obj.addProperty("username", usuario.getUsername());
                obj.add("permisos", gson.toJsonTree(idsPermisos));
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(obj.toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } else {
            // Obtener todos los usuarios
            List<Usuario> usuarios = UsuarioRepository.obtenerTodosUsuarios();
            request.setAttribute("usuarios", usuarios);

            // Solo voluntarios con acceso al sistema y sin usuario asignado
            List<Voluntario> voluntarios = VoluntarioRepository.obtenerVoluntariosConAcceso();
            request.setAttribute("voluntarios", voluntarios);

            // Obtener todos los roles del sistema para el dropdown
            List<RolSistema> roles = RolSistemaRepository.obtenerTodosRoles();
            request.setAttribute("roles", roles);

            // Mapa idUsuario → nombreRol para mostrar en la tabla
            request.setAttribute("rolesUsuario", RolSistemaRepository.obtenerRolesPorUsuario());

            // Obtener todos los permisos disponibles para el modal
            List<Permiso> permisos = PermisoRepository.obtenerTodosPermisos();
            request.setAttribute("permisosDisponibles", permisos);

            request.setAttribute("page", "usuarios");

            request.getRequestDispatcher("/views/usuarios/usuario.html")
                    .forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (UsuarioRepository == null) UsuarioRepository = new UsuarioRepository();
        if (VoluntarioRepository == null) VoluntarioRepository = new VoluntarioRepository();
        if (RolSistemaRepository == null) RolSistemaRepository = new RolSistemaRepository();
        if (PermisoRepository == null) PermisoRepository = new PermisoRepository();

        HttpSession session = request.getSession(false);

        // Verificar si el usuario está logueado
        if (session == null || session.getAttribute("usuarioLogeado") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        response.setContentType("application/json;charset=UTF-8");

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
            boolean creado = UsuarioRepository.registrarUsuarioConVoluntario(voluntarioId, rolSistemaId, username, password);

            if (creado) {
                // Guardar permisos seleccionados
                List<Integer> idsPermisos = parsearPermisos(request.getParameterValues("permisos[]"));
                if (!idsPermisos.isEmpty()) {
                    Usuario nuevoUsuario = UsuarioRepository.obtenerUsuarioPorUsername(username);
                    if (nuevoUsuario != null) {
                        PermisoRepository.guardarPermisosUsuario(nuevoUsuario.getIdUsuario(), idsPermisos);
                    }
                }
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

            // TODO: Actualizar método en UsuarioRepository para editar con voluntarioId y
            // rolSistemaId

            // Guardar permisos actualizados
            List<Integer> idsPermisos = parsearPermisos(request.getParameterValues("permisos[]"));
            PermisoRepository.guardarPermisosUsuario(id, idsPermisos);

            responseJson.addProperty("success", true);
            responseJson.addProperty("message", "Usuario actualizado exitosamente");
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
            boolean actualizado = UsuarioRepository.cambiarEstadoUsuario(id, nuevoEstado);

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

            // Eliminar permisos primero (por FK), luego el usuario
            PermisoRepository.eliminarPermisosUsuario(id);
            boolean eliminado = UsuarioRepository.eliminarUsuario(id);

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

    /**
     * Convierte el array de strings de permisos recibido del formulario
     * en una lista de enteros. Ignora valores nulos o inválidos.
     */
    private List<Integer> parsearPermisos(String[] permisosParam) {
        List<Integer> ids = new ArrayList<>();
        if (permisosParam == null) return ids;
        for (String s : permisosParam) {
            try {
                ids.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return ids;
    }
}
