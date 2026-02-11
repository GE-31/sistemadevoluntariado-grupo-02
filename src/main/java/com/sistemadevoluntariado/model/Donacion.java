package com.sistemadevoluntariado.model;

public class Donacion {

    private int idDonacion;
    private Double cantidad;      // para dinero
    private String descripcion;
    private int idTipoDonacion;
    private String tipoDonacion;
    private int idActividad;
    private String actividad;
    private int idUsuarioRegistro;
    private String usuarioRegistro;
    private String registradoEn;

    public Donacion() {}

    // Getters y Setters
    public int getIdDonacion() {
        return idDonacion;
    }

    public void setIdDonacion(int idDonacion) {
        this.idDonacion = idDonacion;
    }

    public Double getCantidad() {
        return cantidad;
    }

    public void setCantidad(Double cantidad) {
        this.cantidad = cantidad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getIdTipoDonacion() {
        return idTipoDonacion;
    }

    public void setIdTipoDonacion(int idTipoDonacion) {
        this.idTipoDonacion = idTipoDonacion;
    }

    public String getTipoDonacion() {
        return tipoDonacion;
    }

    public void setTipoDonacion(String tipoDonacion) {
        this.tipoDonacion = tipoDonacion;
    }

    public int getIdActividad() {
        return idActividad;
    }

    public void setIdActividad(int idActividad) {
        this.idActividad = idActividad;
    }

    public String getActividad() {
        return actividad;
    }

    public void setActividad(String actividad) {
        this.actividad = actividad;
    }

    public int getIdUsuarioRegistro() {
        return idUsuarioRegistro;
    }

    public void setIdUsuarioRegistro(int idUsuarioRegistro) {
        this.idUsuarioRegistro = idUsuarioRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public String getRegistradoEn() {
        return registradoEn;
    }

    public void setRegistradoEn(String registradoEn) {
        this.registradoEn = registradoEn;
    }
}
