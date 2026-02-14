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
    private String donanteNombre;
    private boolean donacionAnonima;
    private String tipoDonante;
    private String nombreDonante;
    private String correoDonante;
    private String telefonoDonante;
    private Integer idItem;
    private Double cantidadItem;
    private boolean crearNuevoItem;
    private String itemNombre;
    private String itemCategoria;
    private String itemUnidadMedida;
    private Double itemStockMinimo;

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

    public String getDonanteNombre() {
        return donanteNombre;
    }

    public void setDonanteNombre(String donanteNombre) {
        this.donanteNombre = donanteNombre;
    }

    public boolean isDonacionAnonima() {
        return donacionAnonima;
    }

    public void setDonacionAnonima(boolean donacionAnonima) {
        this.donacionAnonima = donacionAnonima;
    }

    public String getTipoDonante() {
        return tipoDonante;
    }

    public void setTipoDonante(String tipoDonante) {
        this.tipoDonante = tipoDonante;
    }

    public String getNombreDonante() {
        return nombreDonante;
    }

    public void setNombreDonante(String nombreDonante) {
        this.nombreDonante = nombreDonante;
    }

    public String getCorreoDonante() {
        return correoDonante;
    }

    public void setCorreoDonante(String correoDonante) {
        this.correoDonante = correoDonante;
    }

    public String getTelefonoDonante() {
        return telefonoDonante;
    }

    public void setTelefonoDonante(String telefonoDonante) {
        this.telefonoDonante = telefonoDonante;
    }

    public Integer getIdItem() {
        return idItem;
    }

    public void setIdItem(Integer idItem) {
        this.idItem = idItem;
    }

    public Double getCantidadItem() {
        return cantidadItem;
    }

    public void setCantidadItem(Double cantidadItem) {
        this.cantidadItem = cantidadItem;
    }

    public boolean isCrearNuevoItem() {
        return crearNuevoItem;
    }

    public void setCrearNuevoItem(boolean crearNuevoItem) {
        this.crearNuevoItem = crearNuevoItem;
    }

    public String getItemNombre() {
        return itemNombre;
    }

    public void setItemNombre(String itemNombre) {
        this.itemNombre = itemNombre;
    }

    public String getItemCategoria() {
        return itemCategoria;
    }

    public void setItemCategoria(String itemCategoria) {
        this.itemCategoria = itemCategoria;
    }

    public String getItemUnidadMedida() {
        return itemUnidadMedida;
    }

    public void setItemUnidadMedida(String itemUnidadMedida) {
        this.itemUnidadMedida = itemUnidadMedida;
    }

    public Double getItemStockMinimo() {
        return itemStockMinimo;
    }

    public void setItemStockMinimo(Double itemStockMinimo) {
        this.itemStockMinimo = itemStockMinimo;
    }
}
