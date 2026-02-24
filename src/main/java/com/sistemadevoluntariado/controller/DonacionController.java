package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.sistemadevoluntariado.entity.Actividad;
import com.sistemadevoluntariado.entity.Donacion;
import com.sistemadevoluntariado.entity.Usuario;
import com.sistemadevoluntariado.repository.ActividadRepository;
import com.sistemadevoluntariado.repository.DonacionRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/donaciones")
public class DonacionController extends HttpServlet {

    private final DonacionRepository donacionRepository = new DonacionRepository();
    private final ActividadRepository actividadRepository = new ActividadRepository();
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class,
            (JsonSerializer<LocalDate>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
        .registerTypeAdapter(LocalDateTime.class,
            (JsonSerializer<LocalDateTime>) (src, t, ctx) -> new JsonPrimitive(src.toString()))
        .create();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Usuario user = (Usuario) req.getSession().getAttribute("usuarioLogeado");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String accion = req.getParameter("accion");
        if ("actividades".equals(accion)) {
            resp.setContentType("application/json;charset=UTF-8");
            List<Actividad> actividades = actividadRepository.obtenerActividadesActivas();
            resp.getWriter().write(gson.toJson(actividades));
            return;
        }
        if ("obtener".equals(accion)) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            Integer id = parseInteger(req.getParameter("id"));
            if (id == null || id <= 0) {
                JsonObject out = new JsonObject();
                out.addProperty("ok", false);
                out.addProperty("message", "ID de donacion invalido");
                resp.getWriter().write(out.toString());
                return;
            }
            Donacion d = donacionRepository.obtenerPorId(id);
            if (d == null) {
                JsonObject out = new JsonObject();
                out.addProperty("ok", false);
                out.addProperty("message", "Donacion no encontrada");
                resp.getWriter().write(out.toString());
                return;
            }
            resp.getWriter().write(gson.toJson(d));
            return;
        }

        String donacionError = (String) req.getSession().getAttribute("donacionError");
        if (donacionError != null) {
            req.setAttribute("donacionError", donacionError);
            req.getSession().removeAttribute("donacionError");
        }

        req.setAttribute("page", "donaciones");
        req.setAttribute("donaciones", donacionRepository.listarTodos());
        req.setAttribute("usuario", user);
        req.getRequestDispatcher("/views/donaciones/donaciones.html").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Usuario user = (Usuario) req.getSession().getAttribute("usuarioLogeado");
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String accion = req.getParameter("accion");
        if ("anular".equals(accion)) {
            procesarAnulacion(req, resp, user);
            return;
        } else if ("editar".equals(accion)) {
            procesarEdicion(req, resp, user);
            return;
        } else if ("cambiar_estado".equals(accion)) {
            procesarCambioEstado(req, resp, user);
            return;
        }

        Donacion d = new Donacion();
        double cantidad = parseDouble(req.getParameter("cantidad"));
        if (cantidad <= 0) {
            resp.sendRedirect("donaciones");
            return;
        }
        d.setCantidad(cantidad);
        d.setDescripcion(req.getParameter("descripcion"));
        int idTipoDonacion = Integer.parseInt(req.getParameter("tipoDonacion"));
        d.setIdTipoDonacion(idTipoDonacion);
        d.setIdActividad(Integer.parseInt(req.getParameter("actividad")));
        d.setIdUsuarioRegistro(user.getIdUsuario());
        boolean donacionAnonima = "1".equals(req.getParameter("donacionAnonima"));
        d.setDonacionAnonima(donacionAnonima);
        if (!donacionAnonima) {
            d.setTipoDonante(trim(req.getParameter("tipoDonante")));
            d.setNombreDonante(trim(req.getParameter("nombreDonante")));
            d.setCorreoDonante(trim(req.getParameter("correoDonante")));
            d.setTelefonoDonante(trim(req.getParameter("telefonoDonante")));
            d.setRucDonante(trim(req.getParameter("rucDonante")));

            String tipoVal = d.getTipoDonante() != null ? d.getTipoDonante().toUpperCase() : "PERSONA";
            if ("PERSONA".equals(tipoVal)) {
                if (d.getNombreDonante() == null || d.getNombreDonante().isEmpty()) {
                    req.getSession().setAttribute("donacionError", "Debe ingresar el nombre del donante (persona).");
                    resp.sendRedirect("donaciones");
                    return;
                }
                if ((d.getCorreoDonante() == null || d.getCorreoDonante().isEmpty())
                        && (d.getTelefonoDonante() == null || d.getTelefonoDonante().isEmpty())) {
                    req.getSession().setAttribute("donacionError", "Para persona: ingresa al menos correo o telefono del donante.");
                    resp.sendRedirect("donaciones");
                    return;
                }
            } else if ("EMPRESA".equals(tipoVal) || "GRUPO".equals(tipoVal)) {
                if (d.getRucDonante() == null || d.getRucDonante().isEmpty()) {
                    req.getSession().setAttribute("donacionError", "Para empresa/grupo es obligatorio el RUC.");
                    resp.sendRedirect("donaciones");
                    return;
                }
                if (d.getNombreDonante() == null || d.getNombreDonante().isEmpty()) {
                    req.getSession().setAttribute("donacionError", "Debe ingresar la razon social del donante (empresa/grupo).");
                    resp.sendRedirect("donaciones");
                    return;
                }
                if ((d.getCorreoDonante() == null || d.getCorreoDonante().isEmpty())
                        && (d.getTelefonoDonante() == null || d.getTelefonoDonante().isEmpty())) {
                    req.getSession().setAttribute("donacionError", "Para empresa/grupo: ingresa al menos correo o telefono del donante.");
                    resp.sendRedirect("donaciones");
                    return;
                }
            }
        }

        if (idTipoDonacion == 2) {
            Integer idItem = parseInteger(req.getParameter("idItem"));
            if (idItem == null || idItem <= 0) {
                req.getSession().setAttribute("donacionError", "Para donaciones en especie debes seleccionar un item existente. Crea el item desde Inventario antes.");
                resp.sendRedirect("donaciones");
                return;
            }

            com.sistemadevoluntariado.repository.InventarioRepository inventarioRepository =
                new com.sistemadevoluntariado.repository.InventarioRepository();
            com.sistemadevoluntariado.entity.InventarioItem itm = inventarioRepository.obtenerPorId(idItem);
            if (itm == null || !"ACTIVO".equalsIgnoreCase(itm.getEstado())) {
                req.getSession().setAttribute("donacionError", "El item seleccionado no existe o no esta activo en el catalogo.");
                resp.sendRedirect("donaciones");
                return;
            }
            d.setIdItem(idItem);
        }

        donacionRepository.guardar(d);
        resp.sendRedirect("donaciones");
    }

    private void procesarEdicion(HttpServletRequest req, HttpServletResponse resp, Usuario user)
            throws IOException {
        Integer idDonacion = parseInteger(req.getParameter("idDonacion"));
        if (idDonacion == null || idDonacion <= 0) {
            resp.sendRedirect("donaciones");
            return;
        }

        Donacion actual = donacionRepository.obtenerPorId(idDonacion);
        if (actual == null || "ANULADO".equalsIgnoreCase(actual.getEstado())) {
            resp.sendRedirect("donaciones");
            return;
        }

        Donacion d = new Donacion();
        d.setIdDonacion(idDonacion);
        d.setIdUsuarioRegistro(user.getIdUsuario());

        double nuevaCantidad = parseDouble(req.getParameter("cantidad"));
        boolean especiePendiente = actual.getIdTipoDonacion() == 2 && "PENDIENTE".equalsIgnoreCase(actual.getEstado());
        if ((actual.getIdTipoDonacion() == 1 || especiePendiente) && nuevaCantidad > 0) {
            d.setCantidad(nuevaCantidad);
        } else {
            d.setCantidad(actual.getCantidad());
        }

        d.setDescripcion(trim(req.getParameter("descripcion")));
        Integer idActividad = parseInteger(req.getParameter("actividad"));
        d.setIdActividad(idActividad != null && idActividad > 0 ? idActividad : actual.getIdActividad());

        boolean donacionAnonima = "1".equals(req.getParameter("donacionAnonima"));
        d.setDonacionAnonima(donacionAnonima);
        if (!donacionAnonima) {
            d.setTipoDonante(trim(req.getParameter("tipoDonante")));
            d.setDniDonante(trim(req.getParameter("dniDonante")));
            d.setNombreDonante(trim(req.getParameter("nombreDonante")));
            d.setCorreoDonante(trim(req.getParameter("correoDonante")));
            d.setTelefonoDonante(trim(req.getParameter("telefonoDonante")));
            d.setRucDonante(trim(req.getParameter("rucDonante")));

            String tipoVal = d.getTipoDonante() != null ? d.getTipoDonante().toUpperCase() : "PERSONA";
            if ("PERSONA".equals(tipoVal)) {
                if (d.getDniDonante() == null || d.getDniDonante().isEmpty()) {
                    req.getSession().setAttribute("donacionError", "Para persona: ingresa el DNI del donante.");
                    resp.sendRedirect("donaciones");
                    return;
                }
                if (d.getNombreDonante() == null || d.getNombreDonante().isEmpty()) {
                    req.getSession().setAttribute("donacionError", "Debe ingresar el nombre del donante (persona).");
                    resp.sendRedirect("donaciones");
                    return;
                }
                if ((d.getCorreoDonante() == null || d.getCorreoDonante().isEmpty())
                        && (d.getTelefonoDonante() == null || d.getTelefonoDonante().isEmpty())) {
                    req.getSession().setAttribute("donacionError", "Para persona: ingresa al menos correo o telefono del donante.");
                    resp.sendRedirect("donaciones");
                    return;
                }
            } else if ("EMPRESA".equals(tipoVal) || "GRUPO".equals(tipoVal)) {
                if (d.getRucDonante() == null || d.getRucDonante().isEmpty()) {
                    req.getSession().setAttribute("donacionError", "Para empresa/grupo es obligatorio el RUC.");
                    resp.sendRedirect("donaciones");
                    return;
                }
                if (d.getNombreDonante() == null || d.getNombreDonante().isEmpty()) {
                    req.getSession().setAttribute("donacionError", "Debe ingresar la razon social del donante (empresa/grupo).");
                    resp.sendRedirect("donaciones");
                    return;
                }
                if ((d.getCorreoDonante() == null || d.getCorreoDonante().isEmpty())
                        && (d.getTelefonoDonante() == null || d.getTelefonoDonante().isEmpty())) {
                    req.getSession().setAttribute("donacionError", "Para empresa/grupo: ingresa al menos correo o telefono del donante.");
                    resp.sendRedirect("donaciones");
                    return;
                }
            }
        }
        d.setMotivoAnulacion(trim(req.getParameter("motivoEdicion")));

        boolean ok = donacionRepository.actualizar(d);
        if (ok && especiePendiente && d.getCantidad() != null && d.getCantidad() > 0) {
            ok = donacionRepository.actualizarDetalleEspecie(idDonacion, d.getCantidad(), d.getDescripcion());
        }
        if (!ok) {
            req.getSession().setAttribute("donacionError", "No se pudo guardar los cambios de la donacion.");
        }

        resp.sendRedirect("donaciones");
    }

    private void procesarAnulacion(HttpServletRequest req, HttpServletResponse resp, Usuario user)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        JsonObject out = new JsonObject();

        Integer idDonacion = parseInteger(req.getParameter("idDonacion"));
        if (idDonacion == null || idDonacion <= 0) {
            out.addProperty("ok", false);
            out.addProperty("message", "ID de donacion invalido");
            resp.getWriter().write(out.toString());
            return;
        }

        String motivo = trim(req.getParameter("motivo"));
        if (motivo == null || motivo.isEmpty()) {
            motivo = "Anulacion manual";
        }

        boolean ok = donacionRepository.anular(idDonacion, user.getIdUsuario(), motivo);
        out.addProperty("ok", ok);
        out.addProperty("message", ok ? "Donacion anulada" : "No se pudo anular la donacion");
        resp.getWriter().write(out.toString());
    }

    private void procesarCambioEstado(HttpServletRequest req, HttpServletResponse resp, Usuario user)
            throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        JsonObject out = new JsonObject();

        Integer idDonacion = parseInteger(req.getParameter("idDonacion"));
        String estado = trim(req.getParameter("estado"));
        if (idDonacion == null || idDonacion <= 0 || estado == null || estado.isEmpty()) {
            out.addProperty("ok", false);
            out.addProperty("message", "Parametros invalidos");
            resp.getWriter().write(out.toString());
            return;
        }

        boolean actualizado;
        if ("CONFIRMADO".equalsIgnoreCase(estado)) {
            actualizado = donacionRepository.confirmar(idDonacion, user.getIdUsuario());
        } else if ("ANULADO".equalsIgnoreCase(estado) || "RECHAZADO".equalsIgnoreCase(estado)) {
            String motivo = trim(req.getParameter("motivo"));
            if (motivo == null || motivo.isEmpty()) {
                motivo = "Anulacion desde cambio de estado";
            }
            actualizado = donacionRepository.anular(idDonacion, user.getIdUsuario(), motivo);
        } else {
            actualizado = donacionRepository.cambiarEstado(idDonacion, estado);
        }

        out.addProperty("ok", actualizado);
        out.addProperty("message", actualizado ? "Estado actualizado" : "No se pudo actualizar el estado");
        resp.getWriter().write(out.toString());
    }

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0d;
        }
        return Double.parseDouble(value);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Integer.parseInt(value);
    }
}
