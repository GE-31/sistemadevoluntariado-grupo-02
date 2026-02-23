package com.sistemadevoluntariado.repository;

import java.util.List;

import com.sistemadevoluntariado.config.PersistenceManager;
import com.sistemadevoluntariado.entity.RolSistema;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;

public class RolSistemaRepository {
    private EntityManagerFactory emf() {
        return PersistenceManager.getEntityManagerFactory();
    }

    public List<RolSistema> obtenerTodosRoles() {
        EntityManager em = emf().createEntityManager();
        try {
            TypedQuery<RolSistema> query = em.createQuery(
                "SELECT r FROM RolSistema r ORDER BY r.nombreRol ASC", RolSistema.class);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        } finally {
            em.close();
        }
    }

    public RolSistema obtenerRolPorId(int idRol) {
        EntityManager em = emf().createEntityManager();
        try {
            return em.find(RolSistema.class, idRol);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

    /**
     * Obtiene el nombre del rol asignado a un usuario.
     * Retorna null si el usuario no tiene rol asignado.
     */
    public String obtenerNombreRolDeUsuario(int idUsuario) {
        EntityManager em = emf().createEntityManager();
        try {
            List<?> resultado = em.createNativeQuery(
                "SELECT rs.nombre_rol FROM usuario_rol ur " +
                "JOIN rol_sistema rs ON ur.id_rol_sistema = rs.id_rol_sistema " +
                "WHERE ur.id_usuario = ? LIMIT 1")
                .setParameter(1, idUsuario)
                .getResultList();
            return resultado.isEmpty() ? null : (String) resultado.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

    /**
     * Devuelve un mapa idUsuario → nombreRol para todos los usuarios.
     */
    public java.util.Map<Integer, String> obtenerRolesPorUsuario() {
        EntityManager em = emf().createEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(
                "SELECT ur.id_usuario, rs.nombre_rol FROM usuario_rol ur " +
                "JOIN rol_sistema rs ON ur.id_rol_sistema = rs.id_rol_sistema")
                .getResultList();
            java.util.Map<Integer, String> mapa = new java.util.HashMap<>();
            for (Object[] row : rows) {
                mapa.put(((Number) row[0]).intValue(), (String) row[1]);
            }
            return mapa;
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.HashMap<>();
        } finally {
            em.close();
        }
    }
}
 