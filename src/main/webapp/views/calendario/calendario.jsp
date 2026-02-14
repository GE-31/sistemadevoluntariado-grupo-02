<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.sistemadevoluntariado.model.Usuario" %>

<%
    HttpSession ses = request.getSession(false);
    Usuario usuario = (Usuario)(request.getAttribute("usuario") != null
        ? request.getAttribute("usuario")
        : (ses != null ? ses.getAttribute("usuarioLogeado") : null));

    if (usuario == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    request.setAttribute("page", "calendario");
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Calendario - Sistema de Voluntariado</title>

    <!-- CSS -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/calendario.css">

    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">

    <!-- FullCalendar -->
    <link href="https://cdn.jsdelivr.net/npm/fullcalendar@6.1.8/index.global.min.css" rel="stylesheet">

</head>
<body>

<!-- SIDEBAR -->
<jsp:include page="/includes/sidebar.jsp" />

<main class="main-content">

    <!-- TOPBAR -->
    <jsp:include page="/includes/topbar.jsp" />

    <div class="content-body">

        <!-- TÍTULO -->
        <div class="titulo-calendario">
            <h1><i class="fa-solid fa-calendar-days"></i> Calendario de Actividades</h1>
            <p>Visualiza toda la programación del voluntariado y tus recordatorios</p>
        </div>

        <!-- CONTENEDOR -->
        <div class="cal-container">
            <div id="calendar"></div>
        </div>

    </div>
</main>

<!-- ════════════════════════════════════════
     MODAL CREAR EVENTO
════════════════════════════════════════ -->
<div id="modalEvento" class="modal-overlay">
    <div class="modal-container">
        <div class="modal-card">

            <!-- Header -->
            <div class="modal-header-enhanced">
                <div>
                    <h2><i class="fa-solid fa-calendar-plus"></i> Registrar Nuevo Evento</h2>
                    <p class="modal-subtitle">Agrega un recordatorio o evento al calendario</p>
                </div>
                <button class="modal-close-btn" onclick="cerrarModal()">✕</button>
            </div>

            <!-- FORMULARIO -->
            <form id="formEvento">

                <div class="form-grid">

                    <!-- Título -->
                    <div class="form-group full-width">
                        <label><i class="fa-solid fa-heading"></i> Título del evento *</label>
                        <input type="text" id="titulo" required
                               placeholder="Ej: Pago de luz, Reunión, Cumpleaños...">
                    </div>

                    <!-- Descripción -->
                    <div class="form-group full-width">
                        <label><i class="fa-solid fa-align-left"></i> Descripción (opcional)</label>
                        <textarea id="descripcion" rows="3"
                                  placeholder="Detalle adicional del evento..."></textarea>
                    </div>

                    <!-- Fecha inicio -->
                    <div class="form-group">
                        <label><i class="fa-solid fa-calendar"></i> Fecha Inicio *</label>
                        <input type="date" id="fechaInicio" required>
                    </div>

                    <!-- Fecha fin -->
                    <div class="form-group">
                        <label><i class="fa-solid fa-calendar-check"></i> Fecha Fin</label>
                        <input type="date" id="fechaFin">
                    </div>

                </div>

                <!-- SELECTOR DE COLOR -->
                <div class="color-selector">
                    <label><i class="fa-solid fa-palette"></i> Seleccionar color:</label>
                    <div class="color-options">
                        <button type="button" class="color-dot active" data-color="#f97316" style="background:#f97316" title="Naranja"></button>
                        <button type="button" class="color-dot" data-color="#eab308" style="background:#eab308" title="Amarillo"></button>
                        <button type="button" class="color-dot" data-color="#22c55e" style="background:#22c55e" title="Verde"></button>
                        <button type="button" class="color-dot" data-color="#06b6d4" style="background:#06b6d4" title="Cyan"></button>
                        <button type="button" class="color-dot" data-color="#6366f1" style="background:#6366f1" title="Indigo"></button>
                        <button type="button" class="color-dot" data-color="#d946ef" style="background:#d946ef" title="Rosa"></button>
                        <button type="button" class="color-dot" data-color="#64748b" style="background:#64748b" title="Gris"></button>
                        <button type="button" class="color-dot" data-color="#ef4444" style="background:#ef4444" title="Rojo"></button>
                    </div>
                </div>
                <input type="hidden" id="colorEvento" value="#f97316">

                <!-- Botones -->
                <div class="modal-actions">
                    <button type="button" class="btn btn-cancel" onclick="cerrarModal()">
                        <i class="fa-solid fa-xmark"></i> Cancelar
                    </button>
                    <button type="submit" class="btn btn-save">
                        <i class="fa-solid fa-floppy-disk"></i> Guardar evento
                    </button>
                </div>
            </form>

        </div>
    </div>
</div>

<!-- Toast de notificación -->
<div id="toastCalendario" class="toast-calendario"></div>

<script>
    const contextPath = '${pageContext.request.contextPath}';
</script>

<!-- FullCalendar -->
<script src="https://cdn.jsdelivr.net/npm/fullcalendar@6.1.8/index.global.min.js"></script>
<script src="${pageContext.request.contextPath}/js/calendario.js"></script>

</body>
</html>
