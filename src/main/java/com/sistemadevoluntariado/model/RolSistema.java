package com.sistemadevoluntariado.model;

public class RolSistema {
    private int idRolSistema;
    private String nombreRol;
    private String descripcion;

    // Constructor vacío
    public RolSistema() {
    }

    // Constructor con parámetros
    public RolSistema(int idRolSistema, String nombreRol, String descripcion) {
        this.idRolSistema = idRolSistema;
        this.nombreRol = nombreRol;
        this.descripcion = descripcion;
    }

    // Getters y Setters
    public int getIdRolSistema() {
        return idRolSistema;
    }

    public void setIdRolSistema(int idRolSistema) {
        this.idRolSistema = idRolSistema;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public void setNombreRol(String nombreRol) {
        this.nombreRol = nombreRol;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return "RolSistema{" +
                "idRolSistema=" + idRolSistema +
                ", nombreRol='" + nombreRol + '\'' +
                ", descripcion='" + descripcion + '\'' +
                '}';
    }
}
