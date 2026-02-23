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
import com.sistemadevoluntariado.repository.TesoreriaRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/donaciones")
public class DonacionController extends HttpServlet {

    DonacionRepository donacionRepository = new DonacionRepository();
    ActividadRepository actividadRepository = new ActividadRepository();
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

        // Si hubo error al intentar crear una donacion (servidor), mostrarlo en la vista
        String donacionError = (String) req.getSession().getAttribute("donacionError");
        if (donacionError != null) {
            req.setAttribute("donacionError", donacionError);
            req.getSession().removeAttribute("donacionError");
        }

        req.setAttribute("page", "donaciones");
        // Mostrar todas las donaciones (incluye PENDIENTE/CONFIRMADO) para permitir cambios de estado desde la vista
        req.setAttribute("donaciones", donacionRepository.listarTodos());
        req.setAttribute("usuario", user);

        req.getRequestDispatcher("/views/donaciones/donaciones.html")
                .forward(req, resp);
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

            // VALIDACIÓN: Persona -> nombre + (correo|telefono); Empresa/Grupo -> RUC + (correo|telefono)
            String tipoVal = d.getTipoDonante() != null ? d.getTipoDonante().toUpperCase() : "PERSONA";
            if ("PERSONA".equals(tipoVal)) {
                if (d.getNombreDonante() == null || d.getNombreDonante().isEmpty()) {
                    req.getSession().setAttribute("donacionError", "Debe ingresar el nombre del donante (persona).");
                    resp.sendRedirect("donaciones");
                    return;
                }
                if ((d.getCorreoDonante() == null || d.getCorreoDonante().isEmpty()) && (d.getTelefonoDonante() == null || d.getTelefonoDonante().isEmpty())) {
                    req.getSession().setAttribute("donacionError", "Para persona: ingresa al menos correo o teléfono del donante.");
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
                    req.getSession().setAttribute("donacionError", "Debe ingresar la razón social del donante (empresa/grupo).");
                    resp.sendRedirect("donaciones");
                    return;
                }
                if ((d.getCorreoDonante() == null || d.getCorreoDonante().isEmpty()) && (d.getTelefonoDonante() == null || d.getTelefonoDonante().isEmpty())) {
                    req.getSession().setAttribute("donacionError", "Para empresa/grupo: ingresa al menos correo o teléfono del donante.");
                    resp.sendRedirect("donaciones");
                    return;
                }
            }
        }

        if (idTipoDonacion == 2) {
            Integer idItem = parseInteger(req.getParameter("idItem"));
            // RESTRICCION: no permitimos crear items desde el formulario de donacion.
            // Para donaciones en especie el item debe existir en el catalogo.
            if (idItem == null || idItem <= 0) {
                req.getSession().setAttribute("donacionError", "Para donaciones en especie debes seleccionar un ítem existente. Crea el ítem desde Inventario antes.");
                resp.sendRedirect("donaciones");
                return;
            }

            // Validar existencia y estado del ítem en servidor
            com.sistemadevoluntariado.repository.InventarioRepository inventarioRepository = new com.sistemadevoluntariado.repository.InventarioRepository();
            com.sistemadevoluntariado.entity.InventarioItem itm = inventarioRepository.obtenerPorId(idItem);
            if (itm == null || !"ACTIVO".equalsIgnoreCase(itm.getEstado())) {
                req.getSession().setAttribute("donacionError", "El ítem seleccionado no existe o no está activo en el catálogo.");
                resp.sendRedirect("donaciones");
                return;
            }

            d.setIdItem(idItem);
        }

        boolean ok = donacionRepository.guardar(d);
        System.out.println("► guardar donacion resultado=" + ok
            + " tipo=" + d.getIdTipoDonacion()
            + " cantidad=" + d.getCantidad()
            + " actividad=" + d.getIdActividad()
            + " anonima=" + d.isDonacionAnonima()
            + " donante=" + d.getNombreDonante());

        // NOTA: No registrar movimiento en Tesorería al crear la donación.
        // El movimiento se genera únicamente cuando la donación pasa a estado CONFIRMADO.
        // (La contabilización se realiza en procesarCambioEstado).

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

            // VALIDACIÓN (mismo criterio que en registro)
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
                if ((d.getCorreoDonante() == null || d.getCorreoDonante().isEmpty()) && (d.getTelefonoDonante() == null || d.getTelefonoDonante().isEmpty())) {
                    req.getSession().setAttribute("donacionError", "Para persona: ingresa al menos correo o teléfono del donante.");
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
                    req.getSession().setAttribute("donacionError", "Debe ingresar la razón social del donante (empresa/grupo).");
                    resp.sendRedirect("donaciones");
                    return;
                }
                if ((d.getCorreoDonante() == null || d.getCorreoDonante().isEmpty()) && (d.getTelefonoDonante() == null || d.getTelefonoDonante().isEmpty())) {
                    req.getSession().setAttribute("donacionError", "Para empresa/grupo: ingresa al menos correo o teléfono del donante.");
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

        // Si fue donación monetaria, actualizar también el movimiento en Tesorería
        if (ok && actual.getIdTipoDonacion() == 1) {
            try {
                TesoreriaRepository tesoreriaRepository = new TesoreriaRepository();
                // obtener movimientos de categoria 'Donaciones' y localizar el vinculado a esta donacion
                java.util.List<com.sistemadevoluntariado.entity.MovimientoFinanciero> listado = tesoreriaRepository.filtrar("INGRESO", "Donaciones", null, null);
                for (com.sistemadevoluntariado.entity.MovimientoFinanciero mv : listado) {
                    if (mv.getDescripcion() != null && mv.getDescripcion().contains("Donacion #" + idDonacion)) {
                        // actualizar monto, descripcion y comprobante
                        mv.setMonto(d.getCantidad() != null ? d.getCantidad() : mv.getMonto());
                        mv.setDescripcion("Donación" + (d.getDescripcion() != null && !d.getDescripcion().isEmpty() ? ": " + d.getDescripcion() : "") + " (Donacion #" + idDonacion + ")");
                        mv.setCategoria("Donaciones");
                        mv.setIdActividad(d.getIdActividad());
                        String tipoDon = d.getTipoDonante() != null ? d.getTipoDonante().toUpperCase() : null;
                        if (d.isDonacionAnonima() || tipoDon == null || "PERSONA".equals(tipoDon)) {
                            mv.setComprobante("BOLETA-" + idDonacion);
                        } else if ("EMPRESA".equals(tipoDon) || "GRUPO".equals(tipoDon)) {
                            mv.setComprobante((d.getRucDonante() != null && !d.getRucDonante().isEmpty()) ? d.getRucDonante() : mv.getComprobante());
                        }
                        tesoreriaRepository.actualizar(mv);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        // Si fue anulada correctamente y fue una donacion monetaria, eliminar el movimiento en Tesorería
        if (ok) {
            try {
                Donacion d = donacionRepository.obtenerPorId(idDonacion);
                if (d != null && d.getIdTipoDonacion() == 1) {
                    TesoreriaRepository tesoreriaRepository = new TesoreriaRepository();
                    java.util.List<com.sistemadevoluntariado.entity.MovimientoFinanciero> listado = tesoreriaRepository.filtrar("INGRESO", "Donaciones", null, null);
                    for (com.sistemadevoluntariado.entity.MovimientoFinanciero mv : listado) {
                        if (mv.getDescripcion() != null && mv.getDescripcion().contains("Donacion #" + idDonacion)) {
                            tesoreriaRepository.eliminar(mv.getIdMovimiento());
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        out.addProperty("ok", ok);
        out.addProperty("message", ok ? "Donacion anulada" : "No se pudo anular la donacion");
        resp.getWriter().write(out.toString());
    }

    // Procesa solicitudes para cambiar el estado de una donación (p.ej. Aprobar)
    private void procesarCambioEstado(HttpServletRequest req, HttpServletResponse resp, Usuario user) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        JsonObject out = new JsonObject();

        Integer idDonacion = parseInteger(req.getParameter("idDonacion"));
        String estado = trim(req.getParameter("estado"));
        if (idDonacion == null || idDonacion <= 0 || estado == null || estado.isEmpty()) {
            out.addProperty("ok", false);
            out.addProperty("message", "Parámetros inválidos");
            resp.getWriter().write(out.toString());
            return;
        }

        // Obtener estado actual antes de la modificación (necesario para acciones asociadas como Tesorería)
        Donacion antes = donacionRepository.obtenerPorId(idDonacion);

        boolean actualizado = donacionRepository.cambiarEstado(idDonacion, estado);
        out.addProperty("ok", actualizado);

     if (actualizado) {
            // Si la donación pasa a CONFIRMADO -> contabilizarla en Tesorería (si es monetaria)
            // o registrar movimiento de inventario (si es en especie)
            try {
                com.sistemadevoluntariado.repository.TesoreriaRepository tesoreriaRepository = new com.sistemadevoluntariado.repository.TesoreriaRepository();
                if ("CONFIRMADO".equalsIgnoreCase(estado) && antes != null && antes.getIdTipoDonacion() == 1) {
                    // Evitar duplicados: buscar movimientos ya vinculados a esta donación
                    boolean existeMov = false;
                    java.util.List<com.sistemadevoluntariado.entity.MovimientoFinanciero> listado = tesoreriaRepository.filtrar("INGRESO", "Donaciones", null, null);
                    for (com.sistemadevoluntariado.entity.MovimientoFinanciero mv : listado) {
                        if (mv.getDescripcion() != null && mv.getDescripcion().contains("Donacion #" + idDonacion)) {
                            existeMov = true;
                            break;
                        }
                    }
                    if (!existeMov) {
                        com.sistemadevoluntariado.entity.MovimientoFinanciero mov = new com.sistemadevoluntariado.entity.MovimientoFinanciero();
                        mov.setTipo("INGRESO");
                        mov.setMonto(antes.getCantidad() != null ? antes.getCantidad() : 0d);
                        mov.setDescripcion("Donación" + (antes.getDescripcion() != null && !antes.getDescripcion().isEmpty() ? ": " + antes.getDescripcion() : "") + " (Donacion #" + idDonacion + ")");
                        mov.setCategoria("Donaciones");
                        String tipoDon = antes.getTipoDonante() != null ? antes.getTipoDonante().toUpperCase() : null;
                        String comprobante = null;
                        if (antes.isDonacionAnonima() || tipoDon == null || "PERSONA".equals(tipoDon)) {
                            comprobante = "BOLETA-" + idDonacion;
                        } else if ("EMPRESA".equals(tipoDon) || "GRUPO".equals(tipoDon)) {
                            comprobante = (antes.getRucDonante() != null && !antes.getRucDonante().isEmpty()) ? antes.getRucDonante() : null;
                        }
                        mov.setComprobante(comprobante);
                        mov.setFechaMovimiento(java.time.LocalDate.now().toString());
                        mov.setIdActividad(antes.getIdActividad());
                        mov.setIdUsuario(user.getIdUsuario());
                        tesoreriaRepository.registrar(mov);
                    }
                }

                // Si la donación en especie pasa a CONFIRMADO -> registrar ENTRADA en inventario
                if ("CONFIRMADO".equalsIgnoreCase(estado) && antes != null && antes.getIdTipoDonacion() == 2
                        && antes.getIdItem() != null && antes.getIdItem() > 0) {
                    com.sistemadevoluntariado.repository.InventarioRepository inventarioRepo = new com.sistemadevoluntariado.repository.InventarioRepository();
                    inventarioRepo.registrarMovimiento(
                        antes.getIdItem(),
                        "ENTRADA",
                        "DONACION",
                        antes.getCantidadItem() != null ? antes.getCantidadItem() : antes.getCantidad(),
                        "Donación en especie #" + idDonacion + (antes.getDescripcion() != null ? " - " + antes.getDescripcion() : ""),
                        user.getIdUsuario()
                    );
                }

                // Si pasa a ANULADO o RECHAZADO -> eliminar movimiento en Tesorería (si existiera)
                if (("ANULADO".equalsIgnoreCase(estado) || "RECHAZADO".equalsIgnoreCase(estado)) && antes != null && antes.getIdTipoDonacion() == 1) {
                    java.util.List<com.sistemadevoluntariado.entity.MovimientoFinanciero> listado = tesoreriaRepository.filtrar("INGRESO", "Donaciones", null, null);
                    for (com.sistemadevoluntariado.entity.MovimientoFinanciero mv : listado) {
                        if (mv.getDescripcion() != null && mv.getDescripcion().contains("Donacion #" + idDonacion)) {
                            tesoreriaRepository.eliminar(mv.getIdMovimiento());
                            break;
                        }
                    }
                }

                // Si donación en especie pasa a ANULADO -> registrar SALIDA para revertir stock
                if (("ANULADO".equalsIgnoreCase(estado) || "RECHAZADO".equalsIgnoreCase(estado)) && antes != null && antes.getIdTipoDonacion() == 2
                        && antes.getIdItem() != null && antes.getIdItem() > 0
                        && "CONFIRMADO".equalsIgnoreCase(antes.getEstado())) {
                    com.sistemadevoluntariado.repository.InventarioRepository inventarioRepo = new com.sistemadevoluntariado.repository.InventarioRepository();
                    inventarioRepo.registrarMovimiento(
                        antes.getIdItem(),
                        "SALIDA",
                        "ANULACION",
                        antes.getCantidadItem() != null ? antes.getCantidadItem() : antes.getCantidad(),
                        "Reversión donación #" + idDonacion,
                        user.getIdUsuario()
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            out.addProperty("message", "Estado actualizado");
        } else {
            out.addProperty("message", "No se pudo actualizar el estado");
        }

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
