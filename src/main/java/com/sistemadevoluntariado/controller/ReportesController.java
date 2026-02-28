package com.sistemadevoluntariado.controller;

import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.sistemadevoluntariado.entity.Donacion;
import com.sistemadevoluntariado.repository.DonacionRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/reportes")
public class ReportesController extends HttpServlet {

    private DonacionRepository DonacionRepository;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (DonacionRepository == null) DonacionRepository = new DonacionRepository();
        String accion = req.getParameter("accion");

        // Mostrar interfaz de reportes cuando no se especifique acción
        if (accion == null || "ui".equalsIgnoreCase(accion) || "index".equalsIgnoreCase(accion)) {
            req.setAttribute("page", "reportes");
            // forward to views/reportes JSP
            req.getRequestDispatcher("/views/reportes/reportes.html").forward(req, resp);
            return;
        }

        // Devolver listados en JSON para búsquedas asíncronas
        if ("listar".equalsIgnoreCase(accion) || "buscar".equalsIgnoreCase(accion)) {
            listarDonacionesJson(req, resp);
            return;
        }

        // Exportar donaciones a XLSX
        if ("donaciones".equalsIgnoreCase(accion) || "export".equalsIgnoreCase(accion)) {
            exportarDonaciones(req, resp);
            return;
        }

        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Accion no soportada");
    }

    // Extrae y aplica filtros comunes a la lista de donaciones
    private java.util.List<Donacion> filtrarDonaciones(HttpServletRequest req) {
        // Obtener todas las donaciones y luego filtrar por fecha + estado CONFIRMADO (ACTIVO se considera legado = CONFIRMADO)
        java.util.List<Donacion> lista = DonacionRepository.listarTodos();

        String fechaInicio = req.getParameter("fechaInicio");
        String fechaFin = req.getParameter("fechaFin");
        String tipoDonacionFilter = req.getParameter("tipoDonacion");
        String tipoDonanteFilter = req.getParameter("tipoDonante");

        // Requerir rango de fechas: si falta inicio o fin -> devolver lista vacía (no mostrar reportes sin rango)
        if (fechaInicio == null || fechaInicio.isEmpty() || fechaFin == null || fechaFin.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        java.time.LocalDate startDate;
        java.time.LocalDate endDate;
        try {
            startDate = java.time.LocalDate.parse(fechaInicio);
            endDate = java.time.LocalDate.parse(fechaFin);
        } catch (java.time.format.DateTimeParseException ex) {
            // formato inválido -> no resultados
            return java.util.Collections.emptyList();
        }

        java.util.stream.Stream<Donacion> stream = lista.stream()
            .filter(d -> {
                String reg = d.getRegistradoEn();
                if (reg == null || reg.isEmpty()) return false;
                // extraer parte fecha YYYY-MM-DD si viene con time
                String datePart = reg.length() >= 10 ? reg.substring(0, 10) : reg;
                try {
                    java.time.LocalDate regDate = java.time.LocalDate.parse(datePart);
                    return (!regDate.isBefore(startDate)) && (!regDate.isAfter(endDate));
                } catch (java.time.format.DateTimeParseException e) {
                    return false;
                }
            });

        if (tipoDonacionFilter != null && !tipoDonacionFilter.isEmpty()) {
            final String td = tipoDonacionFilter;
            stream = stream.filter(d -> String.valueOf(d.getIdTipoDonacion()).equals(td) || (d.getTipoDonacion() != null && d.getTipoDonacion().equalsIgnoreCase(td)));
        }
        if (tipoDonanteFilter != null && !tipoDonanteFilter.isEmpty()) {
            final String tdon = tipoDonanteFilter.toUpperCase();
            stream = stream.filter(d -> d.getTipoDonante() != null && d.getTipoDonante().toUpperCase().equals(tdon));
        }

        // Por defecto, incluir solo donaciones CONFIRMADO (ACTIVO = legacy -> tratado como CONFIRMADO).
        // Si el parámetro 'incluirAnuladas' está presente, también incluimos ANULADO en el listado/export.
        boolean incluirAnuladas = "1".equals(req.getParameter("incluirAnuladas")) || "true".equalsIgnoreCase(req.getParameter("incluirAnuladas"));
        stream = stream.filter(d -> {
            String s = d.getEstado();
            if (s == null || s.trim().isEmpty()) return false;
            s = s.toUpperCase();
            if ("ACTIVO".equals(s)) s = "CONFIRMADO"; // compatibilidad con datos antiguos
            if (incluirAnuladas) {
                return "CONFIRMADO".equals(s) || "ANULADO".equals(s);
            } else {
                return "CONFIRMADO".equals(s);
            }
        });

        return stream.toList();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private void listarDonacionesJson(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        java.util.List<Donacion> filtered = filtrarDonaciones(req);
        resp.setContentType("application/json;charset=UTF-8");
        try (java.io.PrintWriter out = resp.getWriter()) {
            out.print('[');
            boolean first = true;
            for (Donacion d : filtered) {
                if (!first) out.print(',');
                first = false;
                String ident = d.getDniDonante() != null && !d.getDniDonante().isEmpty() ? d.getDniDonante() : (d.getRucDonante() != null ? d.getRucDonante() : "");
                out.print('{');
                out.print("\"idDonacion\":" + d.getIdDonacion() + ',');
                out.print("\"registradoEn\":\"" + escapeJson(d.getRegistradoEn()) + "\",");
                out.print("\"tipoDonacion\":\"" + escapeJson(d.getTipoDonacion()) + "\",");
                out.print("\"cantidad\":" + (d.getCantidad() != null ? d.getCantidad() : 0) + ',');
                out.print("\"tipoDonante\":\"" + escapeJson(d.getTipoDonante()) + "\",");
                out.print("\"dniDonante\":\"" + escapeJson(d.getDniDonante()) + "\",");
                out.print("\"rucDonante\":\"" + escapeJson(d.getRucDonante()) + "\",");
                out.print("\"nombreDonante\":\"" + escapeJson(d.getNombreDonante()) + "\",");
                out.print("\"correoDonante\":\"" + escapeJson(d.getCorreoDonante()) + "\",");
                out.print("\"telefonoDonante\":\"" + escapeJson(d.getTelefonoDonante()) + "\",");
                out.print("\"actividad\":\"" + escapeJson(d.getActividad()) + "\",");
                out.print("\"estado\":\"" + escapeJson(d.getEstado()) + "\"");
                out.print('}');
            }
            out.print(']');
        }
    }

    private void exportarDonaciones(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        java.util.List<Donacion> filtered = filtrarDonaciones(req);

        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment; filename=donaciones.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Donaciones");

            // Estilo header
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFFont font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {"Fecha", "Tipo", "Monto", "TipoDonante", "DNI", "RUC", "Nombre/RazónSocial", "Correo", "Teléfono", "Actividad", "Estado"};

            int rownum = 0;
            Row headerRow = sheet.createRow(rownum++);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            for (Donacion d : filtered) {
                Row r = sheet.createRow(rownum++);
                int c = 0;
                r.createCell(c++).setCellValue(d.getRegistradoEn() != null ? d.getRegistradoEn() : "");
                r.createCell(c++).setCellValue(d.getTipoDonacion() != null ? d.getTipoDonacion() : "");
                r.createCell(c++).setCellValue(d.getCantidad() != null ? d.getCantidad() : 0d);
                r.createCell(c++).setCellValue(d.getTipoDonante() != null ? d.getTipoDonante() : "");
                r.createCell(c++).setCellValue(d.getDniDonante() != null ? d.getDniDonante() : "");
                r.createCell(c++).setCellValue(d.getRucDonante() != null ? d.getRucDonante() : "");
                r.createCell(c++).setCellValue(d.getNombreDonante() != null ? d.getNombreDonante() : "");
                r.createCell(c++).setCellValue(d.getCorreoDonante() != null ? d.getCorreoDonante() : "");
                r.createCell(c++).setCellValue(d.getTelefonoDonante() != null ? d.getTelefonoDonante() : "");
                r.createCell(c++).setCellValue(d.getActividad() != null ? d.getActividad() : "");
                r.createCell(c++).setCellValue(d.getEstado() != null ? d.getEstado() : "");
            }

            // Autosize columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(resp.getOutputStream());
        }
    }
}
