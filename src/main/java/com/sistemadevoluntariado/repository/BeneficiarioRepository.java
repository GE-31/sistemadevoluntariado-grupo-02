package com.sistemadevoluntariado.repository;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.PersistenceManager;
import com.sistemadevoluntariado.entity.Beneficiario;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TypedQuery;

public class BeneficiarioRepository {

    private static final Logger logger = Logger.getLogger(BeneficiarioRepository.class.getName());

    private EntityManagerFactory emf() {
        return PersistenceManager.getEntityManagerFactory();
    }

    // ── CREAR ──────────────────────────────────────────────
    public boolean crearBeneficiario(Beneficiario b) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_crear_beneficiario_adaptado");
            spq.registerStoredProcedureParameter("p_nombres",             String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_apellidos",           String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_dni",                 String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_fecha_nacimiento",    Date.class,    ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_telefono",            String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_direccion",           String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_distrito",            String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_tipo_beneficiario",   String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_necesidad_principal", String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_observaciones",       String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_id_usuario",          Integer.class, ParameterMode.IN);
            spq.setParameter("p_nombres",             b.getNombres());
            spq.setParameter("p_apellidos",           b.getApellidos());
            spq.setParameter("p_dni",                 b.getDni());
            spq.setParameter("p_fecha_nacimiento",    b.getFechaNacimiento() != null ? Date.valueOf(b.getFechaNacimiento()) : null);
            spq.setParameter("p_telefono",            b.getTelefono());
            spq.setParameter("p_direccion",           b.getDireccion());
            spq.setParameter("p_distrito",            b.getDistrito());
            spq.setParameter("p_tipo_beneficiario",   b.getTipoBeneficiario());
            spq.setParameter("p_necesidad_principal", b.getNecesidadPrincipal());
            spq.setParameter("p_observaciones",       b.getObservaciones());
            spq.setParameter("p_id_usuario",          b.getIdUsuario() != null ? b.getIdUsuario() : 0);
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Beneficiario creado correctamente: " + b.getNombres() + " " + b.getApellidos());
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al crear beneficiario: " + e.getMessage(), e);
            return false;
        } finally {
            em.close();
        }
    }

    // ── LISTAR TODOS ───────────────────────────────────────
    @SuppressWarnings("unchecked")
    public List<Beneficiario> obtenerTodosBeneficiarios() {
        EntityManager em = emf().createEntityManager();
        List<Beneficiario> lista = new ArrayList<>();
        try {
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_obtener_todos_beneficiarios", Beneficiario.class);
            spq.execute();
            lista = spq.getResultList();
            logger.info("✓ Se obtuvieron " + lista.size() + " beneficiarios");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener beneficiarios", e);
        } finally {
            em.close();
        }
        return lista;
    }

    // ── OBTENER POR ID ─────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Beneficiario obtenerBeneficiarioPorId(int idBeneficiario) {
        EntityManager em = emf().createEntityManager();
        try {
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_obtener_beneficiario_por_id", Beneficiario.class);
            spq.registerStoredProcedureParameter("p_id_beneficiario", Integer.class, ParameterMode.IN);
            spq.setParameter("p_id_beneficiario", idBeneficiario);
            spq.execute();
            List<?> result = spq.getResultList();
            return result.isEmpty() ? null : (Beneficiario) result.get(0);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener beneficiario por ID", e);
            return null;
        } finally {
            em.close();
        }
    }

    // ── ACTUALIZAR ─────────────────────────────────────────
    public boolean actualizarBeneficiario(Beneficiario b) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_actualizar_beneficiario");
            spq.registerStoredProcedureParameter("p_id_beneficiario",    Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_nombres",            String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_apellidos",          String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_dni",                String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_fecha_nacimiento",   Date.class,    ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_telefono",           String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_direccion",          String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_distrito",           String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_tipo_beneficiario",  String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_necesidad_principal",String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_observaciones",      String.class,  ParameterMode.IN);
            spq.setParameter("p_id_beneficiario",    b.getIdBeneficiario());
            spq.setParameter("p_nombres",            b.getNombres());
            spq.setParameter("p_apellidos",          b.getApellidos());
            spq.setParameter("p_dni",                b.getDni());
            spq.setParameter("p_fecha_nacimiento",   b.getFechaNacimiento() != null ? Date.valueOf(b.getFechaNacimiento()) : null);
            spq.setParameter("p_telefono",           b.getTelefono());
            spq.setParameter("p_direccion",          b.getDireccion());
            spq.setParameter("p_distrito",           b.getDistrito());
            spq.setParameter("p_tipo_beneficiario",  b.getTipoBeneficiario());
            spq.setParameter("p_necesidad_principal",b.getNecesidadPrincipal());
            spq.setParameter("p_observaciones",      b.getObservaciones());
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Beneficiario actualizado correctamente: " + b.getNombres());
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al actualizar beneficiario", e);
            return false;
        } finally {
            em.close();
        }
    }

    // ── CAMBIAR ESTADO ─────────────────────────────────────
    public boolean cambiarEstado(int idBeneficiario, String nuevoEstado) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_cambiar_estado_beneficiario");
            spq.registerStoredProcedureParameter("p_id_beneficiario", Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_estado",          String.class,  ParameterMode.IN);
            spq.setParameter("p_id_beneficiario", idBeneficiario);
            spq.setParameter("p_estado",          nuevoEstado);
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Estado del beneficiario actualizado a: " + nuevoEstado);
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al cambiar estado del beneficiario", e);
            return false;
        } finally {
            em.close();
        }
    }

    // ── ELIMINAR ───────────────────────────────────────────
    public boolean eliminarBeneficiario(int idBeneficiario) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_eliminar_beneficiario");
            spq.registerStoredProcedureParameter("p_id_beneficiario", Integer.class, ParameterMode.IN);
            spq.setParameter("p_id_beneficiario", idBeneficiario);
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Beneficiario eliminado correctamente ID: " + idBeneficiario);
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al eliminar beneficiario", e);
            return false;
        } finally {
            em.close();
        }
    }

    // ── OBTENER POR DNI (sin SP — usa JPQL) ───────────────
    public Beneficiario obtenerPorDni(String dni) {
        EntityManager em = emf().createEntityManager();
        try {
            TypedQuery<Beneficiario> query = em.createQuery(
                "SELECT b FROM Beneficiario b WHERE b.dni = :dni", Beneficiario.class);
            query.setParameter("dni", dni);
            List<Beneficiario> resultados = query.getResultList();
            return resultados.isEmpty() ? null : resultados.get(0);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener beneficiario por DNI", e);
            return null;
        } finally {
            em.close();
        }
    }

    // ── OBTENER ACTIVOS (sin SP — usa JPQL) ───────────────
    public List<Beneficiario> obtenerActivos() {
        EntityManager em = emf().createEntityManager();
        try {
            TypedQuery<Beneficiario> query = em.createQuery(
                "SELECT b FROM Beneficiario b WHERE b.estado = 'ACTIVO' AND b.nombres IS NOT NULL ORDER BY b.idBeneficiario DESC",
                Beneficiario.class);
            return query.getResultList();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener beneficiarios activos", e);
            return List.of();
        } finally {
            em.close();
        }
    }
}
