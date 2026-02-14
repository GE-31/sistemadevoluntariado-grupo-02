package com.sistemadevoluntariado.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sistemadevoluntariado.dao.CertificadoDAO;
import com.sistemadevoluntariado.model.Certificado;
import com.sistemadevoluntariado.model.Usuario;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/certificados/pdf")
public class CertificadoPDFServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final CertificadoDAO certificadoDAO = new CertificadoDAO();

    // ── COLORES ────────────────────────────────────────────
    private static final java.awt.Color COLOR_AZUL_OSCURO = new java.awt.Color(30, 41, 59);
    private static final java.awt.Color COLOR_INDIGO = new java.awt.Color(79, 70, 229);
    private static final java.awt.Color COLOR_INDIGO_CLARO = new java.awt.Color(99, 102, 241);
    private static final java.awt.Color COLOR_GRIS = new java.awt.Color(100, 116, 139);
    private static final java.awt.Color COLOR_GRIS_CLARO = new java.awt.Color(203, 213, 225);
    private static final java.awt.Color COLOR_DORADO = new java.awt.Color(180, 142, 58);
    private static final java.awt.Color COLOR_VERDE = new java.awt.Color(22, 163, 74);
    private static final java.awt.Color COLOR_ROJO = new java.awt.Color(220, 38, 38);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Verificar sesión
        HttpSession session = request.getSession(false);
        Usuario usuario = session != null ? (Usuario) session.getAttribute("usuarioLogeado") : null;
        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String idStr = request.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID de certificado requerido");
            return;
        }

        Certificado cert = certificadoDAO.obtenerCertificadoPorId(Integer.parseInt(idStr));
        if (cert == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Certificado no encontrado");
            return;
        }

        // Generar PDF
        byte[] pdfBytes = generarPDF(cert, request);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "inline; filename=Certificado_" + cert.getCodigoCertificado() + ".pdf");
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }

    // ════════════════════════════════════════════════════════════
    //  GENERAR EL PDF COMPLETO
    // ════════════════════════════════════════════════════════════
    private byte[] generarPDF(Certificado cert, HttpServletRequest request) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Página horizontal A4
        Document doc = new Document(PageSize.A4.rotate(), 50, 50, 40, 40);

        try {
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            PdfContentByte canvas = writer.getDirectContent();

            // ── 1. BORDES DECORATIVOS ──────────────────────
            dibujarBordesDecorativos(canvas, doc);

            // ── 2. LOGO ────────────────────────────────────
            agregarLogo(doc, request);

            // ── 3. TÍTULO ──────────────────────────────────
            doc.add(espaciado(8));

            Font fontTitulo = new Font(Font.HELVETICA, 28, Font.BOLD, COLOR_AZUL_OSCURO);
            Paragraph titulo = new Paragraph("CERTIFICADO DE VOLUNTARIADO", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            doc.add(titulo);

            Font fontSubtitulo = new Font(Font.HELVETICA, 13, Font.NORMAL, COLOR_INDIGO);
            Paragraph subtitulo = new Paragraph("Sistema de Voluntariado Universitario", fontSubtitulo);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            doc.add(subtitulo);

            doc.add(espaciado(6));

            // ── Línea decorativa ───────────────────────────
            dibujarLineaDecorativa(canvas, doc);

            doc.add(espaciado(14));

            // ── 4. TEXTO PRINCIPAL ─────────────────────────
            Font fontTexto = new Font(Font.HELVETICA, 12, Font.NORMAL, COLOR_GRIS);
            Paragraph certifica = new Paragraph("Se certifica que", fontTexto);
            certifica.setAlignment(Element.ALIGN_CENTER);
            doc.add(certifica);

            doc.add(espaciado(8));

            // Nombre del voluntario
            Font fontNombre = new Font(Font.HELVETICA, 24, Font.BOLD, COLOR_AZUL_OSCURO);
            Paragraph nombre = new Paragraph(cert.getNombreVoluntario(), fontNombre);
            nombre.setAlignment(Element.ALIGN_CENTER);
            doc.add(nombre);

            // DNI
            Font fontDni = new Font(Font.HELVETICA, 11, Font.NORMAL, COLOR_GRIS);
            Paragraph dni = new Paragraph("DNI: " + cert.getDniVoluntario(), fontDni);
            dni.setAlignment(Element.ALIGN_CENTER);
            doc.add(dni);

            doc.add(espaciado(10));

            // Participación
            Font fontParticipacion = new Font(Font.HELVETICA, 12, Font.NORMAL, COLOR_GRIS);
            Paragraph participacion = new Paragraph(
                    "Ha participado satisfactoriamente en la actividad de voluntariado",
                    fontParticipacion);
            participacion.setAlignment(Element.ALIGN_CENTER);
            doc.add(participacion);

            doc.add(espaciado(6));

            // Nombre actividad
            Font fontActividad = new Font(Font.HELVETICA, 16, Font.BOLD, COLOR_INDIGO);
            Paragraph actividad = new Paragraph("\"" + cert.getNombreActividad() + "\"", fontActividad);
            actividad.setAlignment(Element.ALIGN_CENTER);
            doc.add(actividad);

            doc.add(espaciado(8));

            // Horas
            Font fontHorasTexto = new Font(Font.HELVETICA, 12, Font.NORMAL, COLOR_GRIS);
            Paragraph horasTexto = new Paragraph("Cumpliendo un total de", fontHorasTexto);
            horasTexto.setAlignment(Element.ALIGN_CENTER);
            doc.add(horasTexto);

            doc.add(espaciado(4));

            // Badge de horas
            agregarBadgeHoras(doc, cert.getHorasVoluntariado());

            doc.add(espaciado(6));

            // Observaciones
            if (cert.getObservaciones() != null && !cert.getObservaciones().trim().isEmpty()) {
                Font fontObs = new Font(Font.HELVETICA, 10, Font.ITALIC, COLOR_GRIS);
                Paragraph obs = new Paragraph(cert.getObservaciones(), fontObs);
                obs.setAlignment(Element.ALIGN_CENTER);
                doc.add(obs);
                doc.add(espaciado(4));
            }

            // Estado ANULADO
            if ("ANULADO".equals(cert.getEstado())) {
                doc.add(espaciado(4));
                Font fontAnulado = new Font(Font.HELVETICA, 36, Font.BOLD, COLOR_ROJO);
                Paragraph anulado = new Paragraph("— CERTIFICADO ANULADO —", fontAnulado);
                anulado.setAlignment(Element.ALIGN_CENTER);
                doc.add(anulado);
                doc.add(espaciado(4));
            }

            doc.add(espaciado(10));

            // ── 5. FIRMA Y QR ──────────────────────────────
            agregarSeccionFirmaYQR(doc, cert, request);

            // ── 6. PIE DE PÁGINA ───────────────────────────
            agregarPiePagina(doc, cert);

            doc.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    // ════════════════════════════════════════════════════════════
    //  BORDES DECORATIVOS (doble borde con esquinas)
    // ════════════════════════════════════════════════════════════
    private void dibujarBordesDecorativos(PdfContentByte canvas, Document doc) {
        float w = doc.getPageSize().getWidth();
        float h = doc.getPageSize().getHeight();

        // Borde exterior dorado
        canvas.setColorStroke(COLOR_DORADO);
        canvas.setLineWidth(3f);
        canvas.rectangle(15, 15, w - 30, h - 30);
        canvas.stroke();

        // Borde interior dorado fino
        canvas.setLineWidth(1f);
        canvas.rectangle(22, 22, w - 44, h - 44);
        canvas.stroke();

        // Esquinas decorativas
        float cornerSize = 30;
        canvas.setLineWidth(2.5f);
        canvas.setColorStroke(COLOR_INDIGO);

        // Esquina superior-izquierda
        canvas.moveTo(28, h - 28);
        canvas.lineTo(28, h - 28 - cornerSize);
        canvas.stroke();
        canvas.moveTo(28, h - 28);
        canvas.lineTo(28 + cornerSize, h - 28);
        canvas.stroke();

        // Esquina superior-derecha
        canvas.moveTo(w - 28, h - 28);
        canvas.lineTo(w - 28, h - 28 - cornerSize);
        canvas.stroke();
        canvas.moveTo(w - 28, h - 28);
        canvas.lineTo(w - 28 - cornerSize, h - 28);
        canvas.stroke();

        // Esquina inferior-izquierda
        canvas.moveTo(28, 28);
        canvas.lineTo(28, 28 + cornerSize);
        canvas.stroke();
        canvas.moveTo(28, 28);
        canvas.lineTo(28 + cornerSize, 28);
        canvas.stroke();

        // Esquina inferior-derecha
        canvas.moveTo(w - 28, 28);
        canvas.lineTo(w - 28, 28 + cornerSize);
        canvas.stroke();
        canvas.moveTo(w - 28, 28);
        canvas.lineTo(w - 28 - cornerSize, 28);
        canvas.stroke();
    }

    // ════════════════════════════════════════════════════════════
    //  LÍNEA DECORATIVA CENTRAL
    // ════════════════════════════════════════════════════════════
    private void dibujarLineaDecorativa(PdfContentByte canvas, Document doc) {
        float w = doc.getPageSize().getWidth();
        float centerX = w / 2;
        float y = doc.getPageSize().getHeight() - 190;
        float lineLength = 120;

        canvas.setColorStroke(COLOR_DORADO);
        canvas.setLineWidth(1.5f);

        // Línea izquierda
        canvas.moveTo(centerX - lineLength - 15, y);
        canvas.lineTo(centerX - 15, y);
        canvas.stroke();

        // Diamante central
        canvas.setColorFill(COLOR_DORADO);
        canvas.moveTo(centerX, y + 4);
        canvas.lineTo(centerX + 6, y);
        canvas.lineTo(centerX, y - 4);
        canvas.lineTo(centerX - 6, y);
        canvas.closePath();
        canvas.fill();

        // Línea derecha
        canvas.moveTo(centerX + 15, y);
        canvas.lineTo(centerX + lineLength + 15, y);
        canvas.stroke();
    }

    // ════════════════════════════════════════════════════════════
    //  AGREGAR LOGO
    // ════════════════════════════════════════════════════════════
    private void agregarLogo(Document doc, HttpServletRequest request) {
        try {
            InputStream logoStream = request.getServletContext().getResourceAsStream("/img/logo.png");
            if (logoStream != null) {
                byte[] logoBytes = logoStream.readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(80, 80);
                logo.setAlignment(Element.ALIGN_CENTER);
                doc.add(logo);
                logoStream.close();
            }
        } catch (Exception e) {
            // Si no se encuentra logo, continuar sin él
            System.out.println("Logo no encontrado, continuando sin logo");
        }
    }

    // ════════════════════════════════════════════════════════════
    //  BADGE DE HORAS
    // ════════════════════════════════════════════════════════════
    private void agregarBadgeHoras(Document doc, int horas) throws Exception {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(25);

        Font fontHoras = new Font(Font.HELVETICA, 18, Font.BOLD, java.awt.Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(horas + " HORAS", fontHoras));
        cell.setBackgroundColor(COLOR_INDIGO);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(10);
        cell.setPaddingBottom(10);
        cell.setBorderWidth(0);
        cell.setBorderColor(java.awt.Color.WHITE);

        table.addCell(cell);
        doc.add(table);
    }

    // ════════════════════════════════════════════════════════════
    //  SECCIÓN DE FIRMA Y CÓDIGO QR
    // ════════════════════════════════════════════════════════════
    private void agregarSeccionFirmaYQR(Document doc, Certificado cert, HttpServletRequest request)
            throws Exception {

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(90);
        table.setWidths(new float[] { 35, 30, 35 });

        // ── Columna 1: Firma del Director ─────────────────
        PdfPCell cellFirma = new PdfPCell();
        cellFirma.setBorderWidth(0);
        cellFirma.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellFirma.setVerticalAlignment(Element.ALIGN_BOTTOM);

        Font fontFirmaLinea = new Font(Font.HELVETICA, 10, Font.NORMAL, COLOR_GRIS_CLARO);
        Font fontFirmaNombre = new Font(Font.HELVETICA, 11, Font.BOLD, COLOR_AZUL_OSCURO);
        Font fontFirmaCargo = new Font(Font.HELVETICA, 9, Font.NORMAL, COLOR_GRIS);

        Paragraph firmaEspacio = new Paragraph("\n\n\n", fontFirmaLinea);
        firmaEspacio.setAlignment(Element.ALIGN_CENTER);
        cellFirma.addElement(firmaEspacio);

        Paragraph firmaLinea = new Paragraph("_________________________", fontFirmaLinea);
        firmaLinea.setAlignment(Element.ALIGN_CENTER);
        cellFirma.addElement(firmaLinea);

        Paragraph firmaNombre = new Paragraph("Director(a) de Voluntariado", fontFirmaNombre);
        firmaNombre.setAlignment(Element.ALIGN_CENTER);
        cellFirma.addElement(firmaNombre);

        Paragraph firmaCargo = new Paragraph("Universidad", fontFirmaCargo);
        firmaCargo.setAlignment(Element.ALIGN_CENTER);
        cellFirma.addElement(firmaCargo);

        table.addCell(cellFirma);

        // ── Columna 2: Código QR ──────────────────────────
        PdfPCell cellQR = new PdfPCell();
        cellQR.setBorderWidth(0);
        cellQR.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellQR.setVerticalAlignment(Element.ALIGN_MIDDLE);

        // Generar QR con URL de verificación
        String urlVerificacion = request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort() + request.getContextPath()
                + "/certificados?action=verificar&codigo=" + cert.getCodigoCertificado();

        Image qrImage = generarCodigoQR(urlVerificacion, 100, 100);
        if (qrImage != null) {
            qrImage.setAlignment(Element.ALIGN_CENTER);
            cellQR.addElement(qrImage);
        }

        Font fontQRLabel = new Font(Font.HELVETICA, 7, Font.NORMAL, COLOR_GRIS);
        Paragraph qrLabel = new Paragraph("Escanea para verificar", fontQRLabel);
        qrLabel.setAlignment(Element.ALIGN_CENTER);
        cellQR.addElement(qrLabel);

        table.addCell(cellQR);

        // ── Columna 3: Firma del Coordinador ──────────────
        PdfPCell cellFirma2 = new PdfPCell();
        cellFirma2.setBorderWidth(0);
        cellFirma2.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellFirma2.setVerticalAlignment(Element.ALIGN_BOTTOM);

        Paragraph firma2Espacio = new Paragraph("\n\n\n", fontFirmaLinea);
        firma2Espacio.setAlignment(Element.ALIGN_CENTER);
        cellFirma2.addElement(firma2Espacio);

        Paragraph firma2Linea = new Paragraph("_________________________", fontFirmaLinea);
        firma2Linea.setAlignment(Element.ALIGN_CENTER);
        cellFirma2.addElement(firma2Linea);

        Paragraph firma2Nombre = new Paragraph("Coordinador(a) de Actividad", fontFirmaNombre);
        firma2Nombre.setAlignment(Element.ALIGN_CENTER);
        cellFirma2.addElement(firma2Nombre);

        Paragraph firma2Cargo = new Paragraph("Responsable de la Actividad", fontFirmaCargo);
        firma2Cargo.setAlignment(Element.ALIGN_CENTER);
        cellFirma2.addElement(firma2Cargo);

        table.addCell(cellFirma2);

        doc.add(table);
    }

    // ════════════════════════════════════════════════════════════
    //  PIE DE PÁGINA
    // ════════════════════════════════════════════════════════════
    private void agregarPiePagina(Document doc, Certificado cert) throws Exception {
        doc.add(espaciado(8));

        // Línea separadora
        PdfPTable lineaTable = new PdfPTable(1);
        lineaTable.setWidthPercentage(80);
        PdfPCell lineaCell = new PdfPCell();
        lineaCell.setBorderWidth(0);
        lineaCell.setBorderWidthTop(0.5f);
        lineaCell.setBorderColorTop(COLOR_GRIS_CLARO);
        lineaCell.setFixedHeight(5);
        lineaTable.addCell(lineaCell);
        doc.add(lineaTable);

        doc.add(espaciado(4));

        // Info del certificado
        PdfPTable footerTable = new PdfPTable(3);
        footerTable.setWidthPercentage(80);
        footerTable.setWidths(new float[] { 33, 34, 33 });

        Font fontFooter = new Font(Font.HELVETICA, 8, Font.NORMAL, COLOR_GRIS);
        Font fontFooterBold = new Font(Font.HELVETICA, 8, Font.BOLD, COLOR_AZUL_OSCURO);

        // Código
        PdfPCell cellCodigo = new PdfPCell();
        cellCodigo.setBorderWidth(0);
        cellCodigo.setHorizontalAlignment(Element.ALIGN_LEFT);
        Paragraph pCodLabel = new Paragraph("Código de Verificación:", fontFooter);
        cellCodigo.addElement(pCodLabel);
        Paragraph pCodigo = new Paragraph(cert.getCodigoCertificado(), fontFooterBold);
        cellCodigo.addElement(pCodigo);
        footerTable.addCell(cellCodigo);

        // Fecha
        PdfPCell cellFecha = new PdfPCell();
        cellFecha.setBorderWidth(0);
        cellFecha.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph pFechaLabel = new Paragraph("Fecha de Emisión:", fontFooter);
        pFechaLabel.setAlignment(Element.ALIGN_CENTER);
        cellFecha.addElement(pFechaLabel);

        String fechaFormateada = cert.getFechaEmision();
        try {
            SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sdfOut = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
            Date fdate = sdfIn.parse(cert.getFechaEmision());
            fechaFormateada = sdfOut.format(fdate);
        } catch (Exception ignored) {
        }

        Paragraph pFecha = new Paragraph(fechaFormateada, fontFooterBold);
        pFecha.setAlignment(Element.ALIGN_CENTER);
        cellFecha.addElement(pFecha);
        footerTable.addCell(cellFecha);

        // Estado
        PdfPCell cellEstado = new PdfPCell();
        cellEstado.setBorderWidth(0);
        cellEstado.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph pEstadoLabel = new Paragraph("Estado:", fontFooter);
        pEstadoLabel.setAlignment(Element.ALIGN_RIGHT);
        cellEstado.addElement(pEstadoLabel);

        Font fontEstado = new Font(Font.HELVETICA, 9, Font.BOLD,
                "EMITIDO".equals(cert.getEstado()) ? COLOR_VERDE : COLOR_ROJO);
        Paragraph pEstado = new Paragraph(cert.getEstado(), fontEstado);
        pEstado.setAlignment(Element.ALIGN_RIGHT);
        cellEstado.addElement(pEstado);
        footerTable.addCell(cellEstado);

        doc.add(footerTable);
    }

    // ════════════════════════════════════════════════════════════
    //  GENERAR CÓDIGO QR
    // ════════════════════════════════════════════════════════════
    private Image generarCodigoQR(String texto, int ancho, int alto) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(texto, BarcodeFormat.QR_CODE, ancho, alto);
            BufferedImage qrBuffered = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
            ImageIO.write(qrBuffered, "PNG", qrBaos);

            return Image.getInstance(qrBaos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════
    //  UTILIDAD: ESPACIADO
    // ════════════════════════════════════════════════════════════
    private Paragraph espaciado(float puntos) {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(puntos);
        p.add(new Chunk(" "));
        return p;
    }
}
