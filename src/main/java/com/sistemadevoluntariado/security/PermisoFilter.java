package com.sistemadevoluntariado.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.sistemadevoluntariado.entity.Usuario;
import com.sistemadevoluntariado.repository.PermisoRepository;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Middleware (Jakarta Filter) que verifica si el usuario autenticado
 * tiene el permiso necesario para acceder a cada ruta del sistema.
 *
 * Rutas protegidas y su permiso requerido:
 *   /voluntarios     → voluntarios.ver
 *   /beneficiarios   → beneficiarios.ver
 *   /actividades     → actividades.ver
 *   /asistencias     → asistencias.ver
 *   /inventario      → inventario.ver
 *   /donaciones      → donaciones.ver
 *   /calendario      → calendario.ver
 *   /reportes        → reportes.ver
 *   /tesoreria       → tesoreria.ver
 *   /certificados    → certificados.ver
 *   /usuarios        → (solo ADMIN: sin restricción de permiso específico)
 *
 * Rutas excluidas (login, recursos estáticos, etc.) no se verifican.
 */
@WebFilter(urlPatterns = {
    "/voluntarios",
    "/beneficiarios",
    "/actividades",
    "/asistencias",
    "/inventario",
    "/donaciones",
    "/calendario",
    "/reportes",
    "/tesoreria",
    "/certificados"
})
public class PermisoFilter implements Filter {

    private static final Logger logger = Logger.getLogger(PermisoFilter.class.getName());

    /**
     * Mapa ruta → nombre del permiso requerido para acceder (GET/vista).
     */
    private static final Map<String, String> RUTA_PERMISO = new HashMap<>();

    static {
        RUTA_PERMISO.put("/voluntarios",   "voluntarios.ver");
        RUTA_PERMISO.put("/beneficiarios", "beneficiarios.ver");
        RUTA_PERMISO.put("/actividades",   "actividades.ver");
        RUTA_PERMISO.put("/asistencias",   "asistencias.ver");
        RUTA_PERMISO.put("/inventario",    "inventario.ver");
        RUTA_PERMISO.put("/donaciones",    "donaciones.ver");
        RUTA_PERMISO.put("/calendario",    "calendario.ver");
        RUTA_PERMISO.put("/reportes",      "reportes.ver");
        RUTA_PERMISO.put("/tesoreria",     "tesoreria.ver");
        RUTA_PERMISO.put("/certificados",  "certificados.ver");
    }

    private PermisoRepository permisoRepository;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.permisoRepository = new PermisoRepository();
        logger.info("✓ PermisoFilter iniciado");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);

        // Si no hay sesión, redirigir al login
        if (session == null || session.getAttribute("usuarioLogeado") == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        Usuario usuarioLogeado = (Usuario) session.getAttribute("usuarioLogeado");

        // Obtener la ruta relativa al contexto (ej. "/voluntarios")
        String contextPath = req.getContextPath();
        String requestURI  = req.getRequestURI();
        String ruta = requestURI.substring(contextPath.length());

        // Quitar query string si existe
        int q = ruta.indexOf('?');
        if (q > 0) ruta = ruta.substring(0, q);

        // Verificar si esta ruta tiene un permiso requerido
        String permisoRequerido = RUTA_PERMISO.get(ruta);

        // Si no hay permiso mapeado para esta ruta, dejar pasar
        if (permisoRequerido == null) {
            chain.doFilter(request, response);
            return;
        }

        // Cachear los permisos del usuario en la sesión para no ir a BD en cada request
        // Si la lista está vacía o nula, siempre recargar desde BD
        @SuppressWarnings("unchecked")
        List<Integer> permisosEnSession = (List<Integer>) session.getAttribute("permisosUsuario");

        if (permisosEnSession == null || permisosEnSession.isEmpty()) {
            permisosEnSession = permisoRepository.obtenerPermisosDeUsuario(usuarioLogeado.getIdUsuario());
            session.setAttribute("permisosUsuario", permisosEnSession);
            // Forzar recarga del rol también
            session.removeAttribute("nombreRolUsuario");
            logger.info("✓ Permisos recargados para " + usuarioLogeado.getUsername() + ": " + permisosEnSession);
        }

        // Verificar el permiso usando los IDs en sesión + nombre del permiso en BD
        boolean tieneAcceso = permisoRepository.tienePermiso(usuarioLogeado.getIdUsuario(), permisoRequerido);

        if (tieneAcceso) {
            chain.doFilter(request, response);
        } else {
            logger.warning("⛔ Usuario " + usuarioLogeado.getUsername() +
                " intentó acceder a " + ruta + " sin permiso: " + permisoRequerido);

            // Redirigir al dashboard con mensaje de acceso denegado
            req.getSession().setAttribute("mensajeError",
                "No tienes permiso para acceder a esa sección.");
            res.sendRedirect(req.getContextPath() + "/dashboard");
        }
    }

    @Override
    public void destroy() {}
}
