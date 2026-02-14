package com.sistemadevoluntariado.controller;

import java.io.IOException;

import com.sistemadevoluntariado.util.CookieUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Eliminar la cookie JWT
        CookieUtil.eliminarCookieAuth(response);

        // Invalidar la sesión HTTP
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Evitar caché para que no se pueda volver con el botón atrás
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        response.sendRedirect(request.getContextPath() + "/login");
    }
}
