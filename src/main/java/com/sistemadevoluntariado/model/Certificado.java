package com.sistemadevoluntariado.model;

public class Certificado {
    private int idCertificado;
    private String codigoCertificado;
    private int idVoluntario;
    private int idActividad;
    private int horasVoluntariado;
    private String fechaEmision;
    private String estado;
    private String observaciones;
    private int idUsuarioEmite;
    private String creadoEn;

    // Campos adicionales para mostrar en vistas (joins)
    private String nombreVoluntario;
    private String dniVoluntario;
    private String nombreActividad;
    private String usuarioEmite;

    // Constructores
    public Certificado() {
    }

    public Certificado(int idVoluntario, int idActividad, int horasVoluntariado) {
        this.idVoluntario = idVoluntario;
        this.idActividad = idActividad;
        this.horasVoluntariado = horasVoluntariado;
        this.estado = "EMITIDO";
    }

    // Getters y Setters
    public int getIdCertificado() {
        return idCertificado;
    }

    public void setIdCertificado(int idCertificado) {
        this.idCertificado = idCertificado;
    }

    public String getCodigoCertificado() {
        return codigoCertificado;
    }

    public void setCodigoCertificado(String codigoCertificado) {
        this.codigoCertificado = codigoCertificado;
    }

    public int getIdVoluntario() {
        return idVoluntario;
    }

    public void setIdVoluntario(int idVoluntario) {
        this.idVoluntario = idVoluntario;
    }

    public int getIdActividad() {
        return idActividad;
    }

    public void setIdActividad(int idActividad) {
        this.idActividad = idActividad;
    }

    public int getHorasVoluntariado() {
        return horasVoluntariado;
    }

    public void setHorasVoluntariado(int horasVoluntariado) {
        this.horasVoluntariado = horasVoluntariado;
    }

    public String getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(String fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public int getIdUsuarioEmite() {
        return idUsuarioEmite;
    }

    public void setIdUsuarioEmite(int idUsuarioEmite) {
        this.idUsuarioEmite = idUsuarioEmite;
    }

    public String getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(String creadoEn) {
        this.creadoEn = creadoEn;
    }

    // Getters para campos de joins
    public String getNombreVoluntario() {
        return nombreVoluntario;
    }

    public void setNombreVoluntario(String nombreVoluntario) {
        this.nombreVoluntario = nombreVoluntario;
    }

    public String getDniVoluntario() {
        return dniVoluntario;
    }

    public void setDniVoluntario(String dniVoluntario) {
        this.dniVoluntario = dniVoluntario;
    }

    public String getNombreActividad() {
        return nombreActividad;
    }

    public void setNombreActividad(String nombreActividad) {
        this.nombreActividad = nombreActividad;
    }

    public String getUsuarioEmite() {
        return usuarioEmite;
    }

    public void setUsuarioEmite(String usuarioEmite) {
        this.usuarioEmite = usuarioEmite;
    }
}
