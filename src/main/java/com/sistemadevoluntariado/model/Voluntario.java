package com.sistemadevoluntariado.model;

public class Voluntario {
    private int idVoluntario;
    private String nombres;
    private String apellidos;
    private String dni;
    private String correo;
    private String telefono;
    private String carrera;
    private String estado;
    private int idUsuario;

    // Constructores
    public Voluntario() {}

    public Voluntario(String nombres, String apellidos, String dni, String correo, String telefono, String carrera) {
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.dni = dni;
        this.correo = correo;
        this.telefono = telefono;
        this.carrera = carrera;
        this.estado = "ACTIVO";
    }

    // Getters y Setters
    public int getIdVoluntario() { return idVoluntario; }
    public void setIdVoluntario(int idVoluntario) { this.idVoluntario = idVoluntario; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCarrera() { return carrera; }
    public void setCarrera(String carrera) { this.carrera = carrera; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
}
