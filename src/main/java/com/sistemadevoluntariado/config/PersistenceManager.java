package com.sistemadevoluntariado.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Expone el EntityManagerFactory de Spring de forma estática
 * para que los Repository (instanciados con "new") puedan usarlo.
 * Si aún no se ha inicializado, retorna null – los Repository
 * deben llamar a getEntityManagerFactory() de forma lazy.
 */
@Component
public class PersistenceManager {

    private static EntityManagerFactory emf;

    @Autowired
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        PersistenceManager.emf = entityManagerFactory;
    }

    /**
     * Retorna el EMF inyectado por Spring. Puede ser null si se llama
     * antes de que Spring termine de arrancar.
     */
    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    /**
     * Retorna el EMF, lanzando excepción si aún no está listo.
     * Usar en operaciones que NECESITAN la BD.
     */
    public static EntityManagerFactory requireEntityManagerFactory() {
        if (emf == null) {
            throw new IllegalStateException("EntityManagerFactory no inicializado. ¿Se inició Spring Boot correctamente?");
        }
        return emf;
    }
}
