package com.sistemadevoluntariado.filter;

import java.io.IOException;
import java.util.logging.Logger;

import com.sistemadevoluntariado.dao.UsuarioDAO;
import com.sistemadevoluntariado.model.Usuario;
import com.sistemadevoluntariado.util.CookieUtil;
import com.sistemadevoluntariado.util.JwtUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Filtro que intercepta todas las peticiones para verificar la autenticación.
 * 
 * Flujo:
 * 1. Si la ruta es pública (login, css, js, img), deja pasar.
 * 2. Busca el token JWT en la cookie AUTH_TOKEN.
 * 3. Si el token es válido, renueva la cookie (sliding expiration) y
 *    establece la sesión del usuario si no existe.
 * 4. Si no hay token o es inválido, redirige a login.
 */
public class AuthFilter implements Filter {

    private static final Logger logger = Logger.getLogger(AuthFilter.class.getName());
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("AuthFilter inicializado");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI().substring(request.getContextPath().length());

        // Rutas públicas que no requieren autenticación
        if (esRutaPublica(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Buscar token en la cookie
        String token = CookieUtil.obtenerTokenDeCookie(request);

        if (token == null || token.isEmpty()) {
            logger.info("No se encontró token, redirigiendo a login");
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Validar token
        Claims claims = JwtUtil.validarToken(token);
        if (claims == null) {
            logger.info("Token inválido o expirado, redirigiendo a login");
            CookieUtil.eliminarCookieAuth(response);
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // Token válido → Renovar cookie (sliding expiration)
        String nuevoToken = JwtUtil.renovarToken(token);
        if (nuevoToken != null) {
            CookieUtil.agregarCookieAuth(response, nuevoToken);
        }

        // Asegurar que la sesión HTTP tenga el usuario cargado
        HttpSession session = request.getSession();
        if (session.getAttribute("usuarioLogeado") == null) {
            int idUsuario = Integer.parseInt(claims.getSubject());
            Usuario usuario = usuarioDAO.obtenerUsuarioPorId(idUsuario);
            if (usuario != null) {
                session.setAttribute("usuarioLogeado", usuario);
            } else {
                // Usuario ya no existe en BD
                CookieUtil.eliminarCookieAuth(response);
                session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Determina si la ruta es pública (no requiere autenticación).
     */
    private boolean esRutaPublica(String path) {
        return path.equals("/login")
                || path.equals("/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/img/")
                || path.startsWith("/fonts/")
                || path.equals("/api/session/renew");
    }

    @Override
    public void destroy() {
        logger.info("AuthFilter destruido");
    }
}
