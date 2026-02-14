package com.sistemadevoluntariado.controller;

import java.io.IOException;

import com.sistemadevoluntariado.util.CookieUtil;
import com.sistemadevoluntariado.util.JwtUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet para renovar la sesión JWT vía petición AJAX.
 * Endpoint: POST /api/session/renew
 * 
 * El frontend llama a este endpoint cada vez que detecta actividad
 * del usuario (clic, mouse move, tecla, etc.) para extender la sesión.
 */
public class SessionRenewServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String token = CookieUtil.obtenerTokenDeCookie(request);

        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"status\":\"expired\",\"message\":\"No hay sesión activa\"}");
            return;
        }

        // Intentar renovar el token
        String nuevoToken = JwtUtil.renovarToken(token);

        if (nuevoToken != null) {
            CookieUtil.agregarCookieAuth(response, nuevoToken);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("{\"status\":\"renewed\",\"message\":\"Sesión renovada\"}");
        } else {
            CookieUtil.eliminarCookieAuth(response);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"status\":\"expired\",\"message\":\"Sesión expirada\"}");
        }
    }
}
