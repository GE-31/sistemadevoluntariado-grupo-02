package com.sistemadevoluntariado.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.sistemadevoluntariado.entity.Usuario;
import com.sistemadevoluntariado.repository.NotificacionRepository;
import com.sistemadevoluntariado.repository.PermisoRepository;
import com.sistemadevoluntariado.service.CustomUserDetailsService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Se ejecuta tras login exitoso:
 *  - guarda el objeto Usuario completo en la sesión HTTP (usuarioLogeado)
 *  - genera notificaciones del día
 *  - redirige al dashboard
 */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String username = authentication.getName();
        Usuario usuario = userDetailsService.loadUsuarioCompleto(username);

        HttpSession session = request.getSession();
        session.setAttribute("usuarioLogeado", usuario);

        // Cargar permisos del usuario en la sesión para el sidebar
        if (usuario != null) {
            try {
                PermisoRepository permisoRepo = new PermisoRepository();
                List<Integer> permisos = permisoRepo.obtenerPermisosDeUsuario(usuario.getIdUsuario());
                session.setAttribute("permisosUsuario", permisos);
            } catch (Exception e) {
                // No cortar el login si los permisos fallan
            }
        }

        // Generar notificaciones del día (misma lógica que AuthServlet anterior)
        if (usuario != null) {
            try {
                NotificacionRepository notifDAO = new NotificacionRepository();
                notifDAO.generarNotificacionesActividadesHoy(usuario.getIdUsuario());
                notifDAO.generarNotificacionesEventosHoy(usuario.getIdUsuario());
            } catch (Exception e) {
                // No cortar el login si las notificaciones fallan
            }
        }

        response.sendRedirect(request.getContextPath() + "/dashboard");
    }
}
