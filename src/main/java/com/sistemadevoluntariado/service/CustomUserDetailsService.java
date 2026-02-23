package com.sistemadevoluntariado.service;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sistemadevoluntariado.repository.UsuarioRepository;
import com.sistemadevoluntariado.entity.Usuario;

/**
 * Carga el usuario desde la BD para Spring Security.
 * El DAO se crea en el momento del login (Spring ya está completamente iniciado).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = Logger.getLogger(CustomUserDetailsService.class.getName());

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("► Spring Security cargando usuario: " + username);

        // Crear DAO lazily — Spring ya está inicializado cuando se llega aquí
        UsuarioRepository dao = new UsuarioRepository();
        Usuario u = dao.obtenerUsuarioPorUsername(username);

        if (u == null) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        }

        boolean activo = !"INACTIVO".equalsIgnoreCase(u.getEstado());

        return new org.springframework.security.core.userdetails.User(
                u.getUsername(),
                u.getPasswordHash(),
                activo,  // enabled
                true,    // accountNonExpired
                true,    // credentialsNonExpired
                activo,  // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    /** Carga el objeto Usuario completo (para guardar en sesión HTTP). */
    public Usuario loadUsuarioCompleto(String username) {
        return new UsuarioRepository().obtenerUsuarioPorUsername(username);
    }
}
