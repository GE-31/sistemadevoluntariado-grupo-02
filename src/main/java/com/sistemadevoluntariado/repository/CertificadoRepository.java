package com.sistemadevoluntariado.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.PersistenceManager;
import com.sistemadevoluntariado.entity.Certificado;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

public class CertificadoRepository {
    private static final Logger logger = Logger.getLogger(CertificadoRepository.class.getName());
    private EntityManagerFactory emf() {
        return PersistenceManager.getEntityManagerFactory();
    }
 
    // ── CREAR ──────────────────────────────────────────────
    public boolean crearCertificado(Certificado c) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_crear_certificado");
            spq.registerStoredProcedureParameter("p_codigo_certificado", String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_id_voluntario",      Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_id_actividad",       Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_horas_voluntariado", Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_observaciones",      String.class,  ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_id_usuario_emite",   Integer.class, ParameterMode.IN);
            spq.setParameter("p_codigo_certificado", c.getCodigoCertificado());
            spq.setParameter("p_id_voluntario",      c.getIdVoluntario());
            spq.setParameter("p_id_actividad",       c.getIdActividad());
            spq.setParameter("p_horas_voluntariado", c.getHorasVoluntariado());
            spq.setParameter("p_observaciones",      c.getObservaciones());
            spq.setParameter("p_id_usuario_emite",   c.getIdUsuarioEmite());
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Certificado creado correctamente");
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al crear certificado: " + e.getMessage(), e);
            return false;
        } finally {
            em.close();
        }
    }

    // ── LISTAR TODOS ───────────────────────────────────────
    // Columnas: [0]id_certificado [1]codigo_certificado [2]id_voluntario [3]id_actividad
    //           [4]horas_voluntariado [5]fecha_emision [6]estado [7]observaciones
    //           [8]id_usuario_emite [9]nombre_voluntario [10]dni_voluntario
    //           [11]nombre_actividad [12]usuario_emite
    @SuppressWarnings("unchecked")
    public List<Certificado> obtenerTodosCertificados() {
        EntityManager em = emf().createEntityManager();
        List<Certificado> lista = new ArrayList<>();
        try {
            List<Object[]> rows = em.createNativeQuery("CALL sp_listar_certificados()").getResultList();
            for (Object[] row : rows) {
                lista.add(mapRow(row));
            }
            logger.info("✓ Se obtuvieron " + lista.size() + " certificados");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener certificados", e);
        } finally {
            em.close();
        }
        return lista;
    }

    private Certificado mapRow(Object[] row) {
        Certificado c = new Certificado();
        c.setIdCertificado(row[0] != null ? ((Number) row[0]).intValue() : 0);
        c.setCodigoCertificado(row[1] != null ? row[1].toString() : null);
        c.setIdVoluntario(row[2] != null ? ((Number) row[2]).intValue() : 0);
        c.setIdActividad(row[3] != null ? ((Number) row[3]).intValue() : 0);
        c.setHorasVoluntariado(row[4] != null ? ((Number) row[4]).intValue() : 0);
        c.setFechaEmision(row[5] != null ? row[5].toString() : null);
        c.setEstado(row[6] != null ? row[6].toString() : null);
        c.setObservaciones(row[7] != null ? row[7].toString() : null);
        c.setIdUsuarioEmite(row[8] != null ? ((Number) row[8]).intValue() : 0);
        c.setNombreVoluntario(row[9] != null ? row[9].toString() : "");
        c.setDniVoluntario(row[10] != null ? row[10].toString() : "");
        c.setNombreActividad(row[11] != null ? row[11].toString() : "");
        c.setUsuarioEmite(row[12] != null ? row[12].toString() : "");
        return c;
    }

    // ── OBTENER POR ID ─────────────────────────────────────
    @SuppressWarnings("unchecked")
    public Certificado obtenerCertificadoPorId(int id) {
        EntityManager em = emf().createEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery("CALL sp_obtener_certificado_por_id(" + id + ")").getResultList();
            return rows.isEmpty() ? null : mapRow(rows.get(0));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener certificado por ID", e);
            return null;
        } finally {
            em.close();
        }
    }

    // ── OBTENER POR CÓDIGO ────────────────────────────────
    @SuppressWarnings("unchecked")
    public Certificado obtenerCertificadoPorCodigo(String codigo) {
        EntityManager em = emf().createEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery("CALL sp_obtener_certificado_por_codigo('" + codigo.replace("'", "''") + "')").getResultList();
            return rows.isEmpty() ? null : mapRow(rows.get(0));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener certificado por código", e);
            return null;
        } finally {
            em.close();
        }
    }

    // ── VERIFICAR DUPLICADO ───────────────────────────────
    public boolean existeCertificadoActivo(int idVoluntario, int idActividad) {
        EntityManager em = emf().createEntityManager();
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(c) FROM Certificado c WHERE c.idVoluntario = :v AND c.idActividad = :a AND c.estado = 'EMITIDO'",
                    Long.class)
                .setParameter("v", idVoluntario)
                .setParameter("a", idActividad)
                .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al verificar duplicado", e);
            return false;
        } finally {
            em.close();
        }
    }

    // ── ANULAR ─────────────────────────────────────────────
    public boolean anularCertificado(int id, String motivo) {
        EntityManager em = emf().createEntityManager();
        try {
            em.getTransaction().begin();
            StoredProcedureQuery spq = em.createStoredProcedureQuery("sp_anular_certificado");
            spq.registerStoredProcedureParameter("p_id_certificado",   Integer.class, ParameterMode.IN);
            spq.registerStoredProcedureParameter("p_motivo_anulacion", String.class,  ParameterMode.IN);
            spq.setParameter("p_id_certificado",   id);
            spq.setParameter("p_motivo_anulacion", motivo);
            spq.execute();
            em.getTransaction().commit();
            logger.info("✓ Certificado anulado correctamente ID: " + id);
            return true;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            logger.log(Level.SEVERE, "✗ Error al anular certificado", e);
            return false;
        } finally {
            em.close();
        }
    }

    // ── OBTENER POR VOLUNTARIO ────────────────────────────
    @SuppressWarnings("unchecked")
    public List<Certificado> obtenerCertificadosPorVoluntario(int idVoluntario) {
        EntityManager em = emf().createEntityManager();
        List<Certificado> lista = new ArrayList<>();
        try {
            List<Object[]> rows = em.createNativeQuery("CALL sp_certificados_por_voluntario(" + idVoluntario + ")").getResultList();
            for (Object[] row : rows) {
                lista.add(mapRow(row));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al obtener certificados por voluntario", e);
        } finally {
            em.close();
        }
        return lista;
    }
}
