package com.sistemadevoluntariado.controller;

import java.io.IOException;

import com.sistemadevoluntariado.dao.NotificacionDAO;
import com.sistemadevoluntariado.dao.UsuarioDAO;
import com.sistemadevoluntariado.model.Usuario;
import com.sistemadevoluntariado.util.CookieUtil;
import com.sistemadevoluntariado.util.JwtUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class AuthServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final NotificacionDAO notificacionDAO = new NotificacionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Si ya tiene un token válido, redirigir al dashboard
        String token = CookieUtil.obtenerTokenDeCookie(request);
        if (token != null && JwtUtil.validarToken(token) != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        request.getRequestDispatcher("/views/auth/login.jsp")
               .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String usuario = request.getParameter("usuario");
        String clave = request.getParameter("clave");

        System.out.println("► Intento de login con usuario: " + usuario);

        if (usuario == null || usuario.isEmpty() || clave == null || clave.isEmpty()) {
            request.setAttribute("error", "Usuario y contraseña son requeridos");
            request.getRequestDispatcher("/views/auth/login.jsp").forward(request, response);
            return;
        }

        // Verificar si la cuenta está bloqueada
        int minutosBloqueo = usuarioDAO.verificarBloqueo(usuario);
        if (minutosBloqueo > 0) {
            System.out.println("Cuenta bloqueada para: " + usuario + " (" + minutosBloqueo + " min restantes)");
            request.setAttribute("error", "Cuenta bloqueada por demasiados intentos fallidos");
            request.getRequestDispatcher("/views/auth/login.jsp").forward(request, response);
            return;
        }

        Usuario user = usuarioDAO.validarLogin(usuario, clave);

        if (user != null) {
            System.out.println("✓ Login exitoso para: " + usuario);

            // Generar token JWT
            String token = JwtUtil.generarToken(
                    user.getIdUsuario(),
                    user.getUsername(),
                    user.getNombres(),
                    user.getApellidos()
            );

            // Establecer cookie persistente con el token
            CookieUtil.agregarCookieAuth(response, token);

            // Establecer sesión HTTP
            HttpSession session = request.getSession();
            session.setAttribute("usuarioLogeado", user);

            // Generar notificaciones de actividades programadas para hoy
            notificacionDAO.generarNotificacionesActividadesHoy(user.getIdUsuario());

            // Generar notificaciones de eventos del calendario para hoy
            notificacionDAO.generarNotificacionesEventosHoy(user.getIdUsuario());

            response.sendRedirect(request.getContextPath() + "/dashboard");
        } else {
            System.out.println("✗ Login fallido para: " + usuario);

            // Registrar intento fallido
            int resultado = usuarioDAO.registrarIntentoFallido(usuario);

            if (resultado == -1) {
                // Se acaba de bloquear la cuenta
                request.setAttribute("error", "Cuenta bloqueada por demasiados intentos fallidos");
            } else if (resultado > 0) {
                int restantes = 3 - resultado;
                request.setAttribute("error", "Usuario o contraseña incorrectos. Intentos restantes: " + restantes);
            } else {
                request.setAttribute("error", "Usuario o contraseña incorrectos");
            }

            request.getRequestDispatcher("/views/auth/login.jsp").forward(request, response);
        }
    }
}

