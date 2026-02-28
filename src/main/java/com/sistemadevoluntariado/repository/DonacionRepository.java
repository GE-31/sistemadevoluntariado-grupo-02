package com.sistemadevoluntariado.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sistemadevoluntariado.config.PersistenceManager;
import com.sistemadevoluntariado.entity.Donacion;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;

public class DonacionRepository {
    private static final Logger logger = Logger.getLogger(DonacionRepository.class.getName());

    private EntityManagerFactory emf() {
        return PersistenceManager.getEntityManagerFactory();
    }

    private static final String SQL_BASE =
        "SELECT d.id_donacion, d.cantidad, d.descripcion, d.id_tipo_donacion, d.id_actividad, " +
        "d.id_usuario_registro, d.registrado_en, " +
        "td.nombre AS tipo_donacion, a.nombre AS nombre_actividad, " +
        "CONCAT(u.nombres,' ',u.apellidos) AS usuario_registro, " +
        "COALESCE(don.nombre, 'Anonimo') AS donante_nombre, " +
        "d.estado, don.tipo AS tipo_donante, " +
        "ddt.id_item, ddt.cantidad AS cantidad_item, " +
        "ii.nombre AS item_nombre, ii.unidad_medida AS item_unidad, " +
        "don.dni, don.ruc, don.correo, don.telefono " +
        "FROM donacion d " +
        "LEFT JOIN tipo_donacion td ON d.id_tipo_donacion = td.id_tipo_donacion " +
        "LEFT JOIN actividades a ON d.id_actividad = a.id_actividad " +
        "LEFT JOIN usuario u ON d.id_usuario_registro = u.id_usuario " +
        "LEFT JOIN donacion_donante dd ON d.id_donacion = dd.id_donacion " +
        "LEFT JOIN donante don ON dd.id_donante = don.id_donante " +
        "LEFT JOIN donacion_detalle ddt ON d.id_donacion = ddt.id_donacion " +
        "LEFT JOIN inventario_item ii ON ddt.id_item = ii.id_item ";

    private Donacion mapear(Object[] row) {
        Donacion d = new Donacion();
        d.setIdDonacion(row[0] != null ? ((Number) row[0]).intValue() : 0);
        d.setCantidad(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
        d.setDescripcion((String) row[2]);
        d.setIdTipoDonacion(row[3] != null ? ((Number) row[3]).intValue() : 0);
        d.setIdActividad(row[4] != null ? ((Number) row[4]).intValue() : 0);
        d.setIdUsuarioRegistro(row[5] != null ? ((Number) row[5]).intValue() : 0);
        d.setRegistradoEn(row[6] != null ? row[6].toString() : null);
        d.setTipoDonacion((String) row[7]);
        d.setActividad((String) row[8]);
        d.setUsuarioRegistro((String) row[9]);
        d.setDonanteNombre((String) row[10]);
        d.setEstado((String) row[11]);
        d.setTipoDonante((String) row[12]);
        d.setIdItem(row[13] != null ? ((Number) row[13]).intValue() : null);
        d.setCantidadItem(row[14] != null ? ((Number) row[14]).doubleValue() : null);
        d.setItemNombre(row[15] != null ? (String) row[15] : null);
        d.setItemUnidadMedida(row[16] != null ? (String) row[16] : null);
        d.setDniDonante(row[17] != null ? (String) row[17] : null);
        d.setRucDonante(row[18] != null ? (String) row[18] : null);
        d.setCorreoDonante(row[19] != null ? (String) row[19] : null);
        d.setTelefonoDonante(row[20] != null ? (String) row[20] : null);
        return d;
    }

    @SuppressWarnings("unchecked")
    public List<Donacion> listar() {
        EntityManager em = emf().createEntityManager();
        try {
            List<Object[]> rows = em.createNativeQuery(SQL_BASE + "ORDER BY d.id_donacion DESC").getResultList();
            List<Donacion> lista = new ArrayList<>();
            for (Object[] row : rows) lista.add(mapear(row));
            return lista;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al listar donaciones", e);
            return List.of();
        } finally {
            em.close();
        }
    }

    public List<Donacion> listarTodos() {
        return listar();
    }

    @SuppressWarnings("unchecked")
    public Donacion obtenerPorId(int id) {
        EntityManager em = emf().createEntityManager();
        try {
            Query query = em.createNativeQuery("{CALL sp_obtener_donacion_detalle(?)}");
            query.setParameter(1, id);
            List<Object[]> rows = query.getResultList();
            return rows.isEmpty() ? null : mapearDetalle(rows.get(0));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al obtener donacion por ID", e);
            return null;
        } finally {
            em.close();
        }
    }

    public boolean guardar(Donacion d) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Query saveQuery = em.createNativeQuery("{CALL sp_guardarDonacion(?,?,?,?,?)}");
            saveQuery.setParameter(1, d.getCantidad());
            saveQuery.setParameter(2, d.getDescripcion());
            saveQuery.setParameter(3, d.getIdTipoDonacion());
            saveQuery.setParameter(4, d.getIdActividad());
            saveQuery.setParameter(5, d.getIdUsuarioRegistro());
            Object saveResult = saveQuery.getSingleResult();
            int idDonacion = extractInt(saveResult);

            // Mantener comportamiento actual del sistema: nuevas donaciones quedan PENDIENTE.
            em.createNativeQuery("UPDATE donacion SET estado = 'PENDIENTE' WHERE id_donacion = ?1")
                .setParameter(1, idDonacion)
                .executeUpdate();

            if (d.isDonacionAnonima()) {
                em.createNativeQuery("DELETE FROM donacion_donante WHERE id_donacion = ?1")
                    .setParameter(1, idDonacion)
                    .executeUpdate();
            } else {
                String tipoDonante = "Persona";
                if (d.getTipoDonante() != null) {
                    String t = d.getTipoDonante().trim().toUpperCase();
                    if ("EMPRESA".equals(t)) tipoDonante = "Empresa";
                    else if ("GRUPO".equals(t)) tipoDonante = "Grupo";
                }
                em.createNativeQuery(
                        "INSERT INTO donante(tipo, nombre, correo, telefono, dni, ruc) VALUES(?1, ?2, ?3, ?4, ?5, ?6)")
                    .setParameter(1, tipoDonante)
                    .setParameter(2, d.getNombreDonante())
                    .setParameter(3, d.getCorreoDonante())
                    .setParameter(4, d.getTelefonoDonante())
                    .setParameter(5, d.getDniDonante())
                    .setParameter(6, d.getRucDonante())
                    .executeUpdate();
                Number idDonante = (Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
                em.createNativeQuery("INSERT INTO donacion_donante(id_donacion, id_donante) VALUES(?1, ?2)")
                    .setParameter(1, idDonacion)
                    .setParameter(2, idDonante.intValue())
                    .executeUpdate();
            }

            if (d.getIdTipoDonacion() == 2 && d.getIdItem() != null && d.getIdItem() > 0) {
                em.createNativeQuery(
                    "INSERT INTO donacion_detalle(id_donacion, id_item, cantidad, observacion, creado_en) VALUES(?1, ?2, ?3, ?4, NOW())")
                    .setParameter(1, idDonacion)
                    .setParameter(2, d.getIdItem())
                    .setParameter(3, d.getCantidad())
                    .setParameter(4, d.getDescripcion())
                    .executeUpdate();
            }

            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al guardar donacion", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean actualizar(Donacion d) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Donacion actual = em.find(Donacion.class, d.getIdDonacion());
            if (actual == null || "ANULADO".equalsIgnoreCase(actual.getEstado())) {
                tx.rollback();
                return false;
            }

            em.createNativeQuery("{CALL sp_actualizarDonacion(?,?,?,?,?)}")
                .setParameter(1, d.getIdDonacion())
                .setParameter(2, d.getCantidad())
                .setParameter(3, d.getDescripcion())
                .setParameter(4, actual.getIdTipoDonacion())
                .setParameter(5, d.getIdActividad())
                .getResultList();

            if (d.isDonacionAnonima()) {
                em.createNativeQuery("DELETE FROM donacion_donante WHERE id_donacion = ?1")
                    .setParameter(1, d.getIdDonacion())
                    .executeUpdate();
            } else {
                List<?> linkedRows = em.createNativeQuery(
                        "SELECT id_donante FROM donacion_donante WHERE id_donacion = ?1 LIMIT 1")
                    .setParameter(1, d.getIdDonacion())
                    .getResultList();

                String tipoDonante = "Persona";
                if (d.getTipoDonante() != null) {
                    String t = d.getTipoDonante().trim().toUpperCase();
                    if ("EMPRESA".equals(t)) tipoDonante = "Empresa";
                    else if ("GRUPO".equals(t)) tipoDonante = "Grupo";
                }

                if (linkedRows.isEmpty()) {
                    em.createNativeQuery(
                            "INSERT INTO donante(tipo, nombre, correo, telefono, dni, ruc) VALUES(?1, ?2, ?3, ?4, ?5, ?6)")
                        .setParameter(1, tipoDonante)
                        .setParameter(2, d.getNombreDonante())
                        .setParameter(3, d.getCorreoDonante())
                        .setParameter(4, d.getTelefonoDonante())
                        .setParameter(5, d.getDniDonante())
                        .setParameter(6, d.getRucDonante())
                        .executeUpdate();
                    Number newDonante = (Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
                    em.createNativeQuery("INSERT INTO donacion_donante(id_donacion, id_donante) VALUES(?1, ?2)")
                        .setParameter(1, d.getIdDonacion())
                        .setParameter(2, newDonante.intValue())
                        .executeUpdate();
                } else {
                    int idDonante = ((Number) linkedRows.get(0)).intValue();
                    em.createNativeQuery(
                            "UPDATE donante SET tipo = ?1, nombre = ?2, correo = ?3, telefono = ?4, dni = ?5, ruc = ?6 WHERE id_donante = ?7")
                        .setParameter(1, tipoDonante)
                        .setParameter(2, d.getNombreDonante())
                        .setParameter(3, d.getCorreoDonante())
                        .setParameter(4, d.getTelefonoDonante())
                        .setParameter(5, d.getDniDonante())
                        .setParameter(6, d.getRucDonante())
                        .setParameter(7, idDonante)
                        .executeUpdate();
                }
            }

            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al actualizar donacion", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean actualizarDetalleEspecie(int idDonacion, double cantidad, String observacion) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int filas = em.createNativeQuery(
                    "UPDATE donacion_detalle SET cantidad = ?1, observacion = ?2 WHERE id_donacion = ?3")
                .setParameter(1, cantidad)
                .setParameter(2, observacion)
                .setParameter(3, idDonacion)
                .executeUpdate();
            tx.commit();
            return filas > 0;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al actualizar detalle de donacion en especie", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean anular(int id, int idUsuario, String motivo) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createNativeQuery("{CALL sp_anular_donacion_inventario(?,?,?)}")
                .setParameter(1, id)
                .setParameter(2, idUsuario)
                .setParameter(3, motivo)
                .executeUpdate();
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al anular donacion", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean confirmar(int id, int idUsuario) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createNativeQuery("{CALL sp_confirmar_donacion_inventario(?,?)}")
                .setParameter(1, id)
                .setParameter(2, idUsuario)
                .executeUpdate();
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al confirmar donacion", e);
            return false;
        } finally {
            em.close();
        }
    }

    public boolean cambiarEstado(int id, String estado) {
        EntityManager em = emf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Donacion d = em.find(Donacion.class, id);
            if (d == null) {
                tx.rollback();
                return false;
            }
            d.setEstado(estado);
            em.merge(d);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            logger.log(Level.SEVERE, "Error al cambiar estado de donacion", e);
            return false;
        } finally {
            em.close();
        }
    }

    private Donacion mapearDetalle(Object[] row) {
        Donacion d = new Donacion();
        d.setIdDonacion(row[0] != null ? ((Number) row[0]).intValue() : 0);
        d.setCantidad(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
        d.setDescripcion(row[2] != null ? String.valueOf(row[2]) : null);
        d.setIdTipoDonacion(row[3] != null ? ((Number) row[3]).intValue() : 0);
        d.setTipoDonacion(row[4] != null ? String.valueOf(row[4]) : null);
        d.setIdActividad(row[5] != null ? ((Number) row[5]).intValue() : 0);
        d.setActividad(row[6] != null ? String.valueOf(row[6]) : null);
        d.setIdUsuarioRegistro(row[7] != null ? ((Number) row[7]).intValue() : 0);
        d.setUsuarioRegistro(row[8] != null ? String.valueOf(row[8]) : null);
        d.setDonacionAnonima(row[9] != null && ((Number) row[9]).intValue() == 1);
        d.setTipoDonante(row[10] != null ? String.valueOf(row[10]) : null);
        d.setNombreDonante(row[11] != null ? String.valueOf(row[11]) : null);
        d.setCorreoDonante(row[12] != null ? String.valueOf(row[12]) : null);
        d.setTelefonoDonante(row[13] != null ? String.valueOf(row[13]) : null);
        d.setDniDonante(row[14] != null ? String.valueOf(row[14]) : null);
        d.setRucDonante(row[15] != null ? String.valueOf(row[15]) : null);
        d.setIdItem(row[16] != null ? ((Number) row[16]).intValue() : null);
        d.setCantidadItem(row[17] != null ? ((Number) row[17]).doubleValue() : null);
        d.setItemNombre(row[18] != null ? String.valueOf(row[18]) : null);
        d.setItemCategoria(row[19] != null ? String.valueOf(row[19]) : null);
        d.setItemUnidadMedida(row[20] != null ? String.valueOf(row[20]) : null);
        d.setEstado(row[21] != null ? String.valueOf(row[21]) : null);
        return d;
    }

    private int extractInt(Object result) {
        if (result == null) return 0;
        if (result instanceof Number n) return n.intValue();
        if (result instanceof Object[] row && row.length > 0 && row[0] instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}
