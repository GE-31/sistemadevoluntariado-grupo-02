package com.sistemadevoluntariado.controller;

import java.io.IOException;

import com.sistemadevoluntariado.dao.NotificacionDAO;
import com.sistemadevoluntariado.dao.UsuarioDAO;
import com.sistemadevoluntariado.model.Usuario;

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

        Usuario user = usuarioDAO.validarLogin(usuario, clave);

        if (user != null) {
            System.out.println("✓ Login exitoso para: " + usuario);
            HttpSession session = request.getSession();
            session.setAttribute("usuarioLogeado", user);

            // Generar notificaciones de actividades programadas para hoy
            notificacionDAO.generarNotificacionesActividadesHoy(user.getIdUsuario());

            // Generar notificaciones de eventos del calendario para hoy
            notificacionDAO.generarNotificacionesEventosHoy(user.getIdUsuario());

            response.sendRedirect(request.getContextPath() + "/dashboard");
        } else {
            System.out.println("✗ Login fallido para: " + usuario);
            request.setAttribute("error", "Usuario o contraseña incorrectos");
            request.getRequestDispatcher("/views/auth/login.jsp").forward(request, response);
        }
    }
}

