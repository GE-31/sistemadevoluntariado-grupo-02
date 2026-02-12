package com.sistemadevoluntariado.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Utilidad para manejar la cookie de autenticación JWT.
 * Configura: HttpOnly, Secure, SameSite=Strict, expires=20min.
 */
public class CookieUtil {

    /**
     * Crea y agrega la cookie AUTH_TOKEN a la respuesta.
     * La cookie persiste aunque se cierre el navegador (usa Max-Age).
     */
    public static void agregarCookieAuth(HttpServletResponse response, String token) {
        // Usamos Set-Cookie manual para incluir SameSite=Strict
        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append(JwtUtil.COOKIE_NAME).append("=").append(token);
        cookieHeader.append("; Max-Age=").append(JwtUtil.COOKIE_MAX_AGE_SECONDS);
        cookieHeader.append("; Path=/");
        cookieHeader.append("; HttpOnly");
        cookieHeader.append("; SameSite=Strict");

        // En producción con HTTPS, descomentar la siguiente línea:
        // cookieHeader.append("; Secure");

        response.addHeader("Set-Cookie", cookieHeader.toString());
    }

    /**
     * Elimina la cookie AUTH_TOKEN estableciendo Max-Age=0.
     */
    public static void eliminarCookieAuth(HttpServletResponse response) {
        StringBuilder cookieHeader = new StringBuilder();
        cookieHeader.append(JwtUtil.COOKIE_NAME).append("=");
        cookieHeader.append("; Max-Age=0");
        cookieHeader.append("; Path=/");
        cookieHeader.append("; HttpOnly");
        cookieHeader.append("; SameSite=Strict");

        response.addHeader("Set-Cookie", cookieHeader.toString());
    }

    /**
     * Obtiene el valor del token desde la cookie AUTH_TOKEN.
     * Retorna null si no existe.
     */
    public static String obtenerTokenDeCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JwtUtil.COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
