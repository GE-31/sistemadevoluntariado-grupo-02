<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.sistemadevoluntariado.model.Usuario" %>
<%@ page import="com.sistemadevoluntariado.model.Asistencia" %>
<%@ page import="com.sistemadevoluntariado.model.Actividad" %>
<%@ page import="com.sistemadevoluntariado.model.Voluntario" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>

<%
    HttpSession ses = request.getSession(false);
    Usuario usuario = (Usuario)(request.getAttribute("usuario") != null
        ? request.getAttribute("usuario")
        : (ses != null ? ses.getAttribute("usuarioLogeado") : null));
    if (usuario == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
    List<Asistencia> asistencias = (List<Asistencia>) request.getAttribute("asistencias");
    if (asistencias == null) asistencias = new ArrayList<>();

    List<Actividad> actividades = (List<Actividad>) request.getAttribute("actividades");
    if (actividades == null) actividades = new ArrayList<>();

    List<Voluntario> voluntarios = (List<Voluntario>) request.getAttribute("voluntarios");
    if (voluntarios == null) voluntarios = new ArrayList<>();

    request.setAttribute("page", "asistencias");
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Asistencias - Sistema de Voluntariado</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/asistencias.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sidebar.css">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
</head>
<body>
    <jsp:include page="/includes/sidebar.jsp" />

    <main class="main-content">
        <jsp:include page="/includes/topbar.jsp" />

        <div class="content-body">

            <!-- Header -->
            <div class="asistencias-header">
                <div>
                    <h1>Control de Asistencias</h1>
                    <p>Registra y controla la asistencia de voluntarios a cada actividad</p>
                </div>
                <div class="header-actions">
                    <select id="filtroActividad" class="filtro-select" onchange="filtrarPorActividad()">
                        <option value="">Todas las actividades</option>
                        <% for (Actividad act : actividades) { %>
                            <option value="<%= act.getIdActividad() %>"><%= act.getNombre() %></option>
                        <% } %>
                    </select>
                    <button class="btn btn-primary" onclick="abrirModalRegistrar()">
                        <span>+</span> Registrar Asistencia
                    </button>
                </div>
            </div>

            <!-- Tarjetas de resumen -->
            <div class="stats-cards">
                <div class="stat-card total">
                    <div class="stat-icon">📋</div>
                    <div class="stat-info">
                        <span class="stat-number" id="statTotal"><%= asistencias.size() %></span>
                        <span class="stat-label">Total Registros</span>
                    </div>
                </div>
                <div class="stat-card asistio">
                    <div class="stat-icon">✅</div>
                    <div class="stat-info">
                        <span class="stat-number" id="statAsistio">
                            <%= asistencias.stream().filter(a -> "ASISTIO".equals(a.getEstado())).count() %>
                        </span>
                        <span class="stat-label">Asistieron</span>
                    </div>
                </div>
                <div class="stat-card tardanza">
                    <div class="stat-icon">⏰</div>
                    <div class="stat-info">
                        <span class="stat-number" id="statTardanza">
                            <%= asistencias.stream().filter(a -> "TARDANZA".equals(a.getEstado())).count() %>
                        </span>
                        <span class="stat-label">Tardanzas</span>
                    </div>
                </div>
                <div class="stat-card falta">
                    <div class="stat-icon">❌</div>
                    <div class="stat-info">
                        <span class="stat-number" id="statFalta">
                            <%= asistencias.stream().filter(a -> "FALTA".equals(a.getEstado())).count() %>
                        </span>
                        <span class="stat-label">Faltas</span>
                    </div>
                </div>
            </div>

            <!-- Tabla de asistencias -->
            <div class="asistencias-table">
                <table>
                    <thead>
                        <tr>
                            <th>Voluntario</th>
                            <th>DNI</th>
                            <th>Actividad</th>
                            <th>Fecha</th>
                            <th>Entrada</th>
                            <th>Salida</th>
                            <th>Horas</th>
                            <th>Estado</th>
                            <th>Acciones</th>
                        </tr>
                    </thead>
                    <tbody id="asistencias-tbody">
                        <% if (!asistencias.isEmpty()) {
                            for (Asistencia a : asistencias) { %>
                            <tr class="asistencia-row" data-id="<%= a.getIdAsistencia() %>">
                                <td><strong><%= a.getNombreVoluntario() %></strong></td>
                                <td><span class="badge-dni"><%= a.getDniVoluntario() != null ? a.getDniVoluntario() : "-" %></span></td>
                                <td><%= a.getNombreActividad() %></td>
                                <td><%= a.getFecha() %></td>
                                <td><%= a.getHoraEntrada() != null ? a.getHoraEntrada() : "-" %></td>
                                <td><%= a.getHoraSalida() != null ? a.getHoraSalida() : "-" %></td>
                                <td><%= a.getHorasTotales() != null ? a.getHorasTotales() : "0.00" %></td>
                                <td>
                                    <span class="estado-badge <%= a.getEstado().toLowerCase() %>">
                                        <%= a.getEstado() %>
                                    </span>
                                </td>
                                <td class="acciones-cell">
                                    <button class="btn-icon edit" onclick="abrirModalEditar(<%= a.getIdAsistencia() %>)" title="Editar">✎</button>
                                    <button class="btn-icon delete" onclick="eliminarAsistencia(<%= a.getIdAsistencia() %>)" title="Eliminar">🗑</button>
                                </td>
                            </tr>
                        <% } } else { %>
                            <tr id="sinAsistenciasRow">
                                <td colspan="9" style="text-align:center; padding:2rem; color:#999;">No hay asistencias registradas</td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>

            <!-- Paginación -->
            <div id="paginacionAsistencias" style="display:flex; justify-content:flex-end; align-items:center; padding:1rem 1.5rem; background:#fff; border-top:1px solid #f0f0f0; border-radius:0 0 12px 12px; gap:0.5rem; font-family:'Inter',sans-serif;">
                <span id="paginacionInfo" style="margin-right:auto; font-size:0.85rem; color:#6b7280;"></span>
                <button id="btnPrevAsis" onclick="cambiarPaginaAsis(-1)" disabled style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.5rem 0.9rem; border:1px solid #e5e7eb; background:#fff; color:#4b5563; border-radius:8px; font-size:0.82rem; font-weight:500; cursor:pointer;">
                    ◀ Anterior
                </button>
                <div id="paginacionPages" style="display:flex; gap:0.25rem;"></div>
                <button id="btnNextAsis" onclick="cambiarPaginaAsis(1)" disabled style="display:inline-flex; align-items:center; gap:0.4rem; padding:0.5rem 0.9rem; border:1px solid #e5e7eb; background:#fff; color:#4b5563; border-radius:8px; font-size:0.82rem; font-weight:500; cursor:pointer;">
                    Siguiente ▶
                </button>
            </div>

        </div>
    </main>

    <!-- ========================================================= -->
    <!-- MODAL REGISTRAR / EDITAR ASISTENCIA                       -->
    <!-- ========================================================= -->
    <div id="modalAsistencia" class="modal-overlay">
        <div class="modal-container">
            <div class="modal-card">
                <!-- Header -->
                <div class="modal-header-enhanced">
                    <div class="modal-header-content">
                        <h2 id="modalTitulo" class="modal-title">Registrar Asistencia</h2>
                        <p class="modal-subtitle">Ingresa los datos de la asistencia</p>
                    </div>
                    <button class="modal-close-btn" onclick="cerrarModal()" title="Cerrar">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <line x1="18" y1="6" x2="6" y2="18"></line>
                            <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                    </button>
                </div>

                <!-- Form -->
                <form id="formAsistencia" onsubmit="guardarAsistencia(event)" class="modal-form">
                    <input type="hidden" id="asistenciaId">
                    <input type="hidden" id="modoEdicion" value="false">

                    <div class="form-grid">
                        <!-- Voluntario -->
                        <div class="form-group" id="grupoVoluntario">
                            <label for="id_voluntario" class="form-label">
                                <span class="label-text">Voluntario</span>
                                <span class="required-indicator">*</span>
                            </label>
                            <div class="input-wrapper">
                                <select id="id_voluntario" name="id_voluntario" class="form-input" required>
                                    <option value="">Seleccione un voluntario</option>
                                    <% for (Voluntario v : voluntarios) {
                                        if ("ACTIVO".equals(v.getEstado())) { %>
                                        <option value="<%= v.getIdVoluntario() %>">
                                            <%= v.getNombres() %> <%= v.getApellidos() %> - <%= v.getDni() %>
                                        </option>
                                    <% } } %>
                                </select>
                            </div>
                        </div>

                        <!-- Actividad -->
                        <div class="form-group" id="grupoActividad">
                            <label for="id_actividad" class="form-label">
                                <span class="label-text">Actividad</span>
                                <span class="required-indicator">*</span>
                            </label>
                            <div class="input-wrapper">
                                <select id="id_actividad" name="id_actividad" class="form-input" required>
                                    <option value="">Seleccione una actividad</option>
                                    <% for (Actividad act : actividades) { %>
                                        <option value="<%= act.getIdActividad() %>"><%= act.getNombre() %></option>
                                    <% } %>
                                </select>
                            </div>
                        </div>

                        <!-- Fecha -->
                        <div class="form-group" id="grupoFecha">
                            <label for="fecha" class="form-label">
                                <span class="label-text">Fecha</span>
                                <span class="required-indicator">*</span>
                            </label>
                            <div class="input-wrapper">
                                <input type="date" id="fecha" name="fecha" class="form-input" required>
                            </div>
                        </div>

                        <!-- Estado -->
                        <div class="form-group">
                            <label for="estado" class="form-label">
                                <span class="label-text">Estado</span>
                                <span class="required-indicator">*</span>
                            </label>
                            <div class="input-wrapper">
                                <select id="estado" name="estado" class="form-input" required onchange="toggleHoras()">
                                    <option value="">Seleccione estado</option>
                                    <option value="ASISTIO">Asistió</option>
                                    <option value="TARDANZA">Tardanza</option>
                                    <option value="FALTA">Falta</option>
                                </select>
                            </div>
                        </div>

                        <!-- Hora Entrada -->
                        <div class="form-group" id="grupoHoraEntrada">
                            <label for="hora_entrada" class="form-label">
                                <span class="label-text">Hora de Entrada</span>
                            </label>
                            <div class="input-wrapper">
                                <input type="time" id="hora_entrada" name="hora_entrada" class="form-input">
                            </div>
                        </div>

                        <!-- Hora Salida -->
                        <div class="form-group" id="grupoHoraSalida">
                            <label for="hora_salida" class="form-label">
                                <span class="label-text">Hora de Salida</span>
                            </label>
                            <div class="input-wrapper">
                                <input type="time" id="hora_salida" name="hora_salida" class="form-input">
                            </div>
                        </div>

                        <!-- Observaciones -->
                        <div class="form-group full-width">
                            <label for="observaciones" class="form-label">
                                <span class="label-text">Observaciones</span>
                            </label>
                            <div class="input-wrapper">
                                <textarea id="observaciones" name="observaciones" class="form-input" rows="3" placeholder="Observaciones adicionales..."></textarea>
                            </div>
                        </div>
                    </div>

                    <!-- Footer -->
                    <div class="modal-footer-enhanced">
                        <button type="button" class="btn-cancel" onclick="cerrarModal()">
                            <span>Cancelar</span>
                        </button>
                        <button type="submit" class="btn-submit">
                            <span>Guardar Asistencia</span>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="${pageContext.request.contextPath}/js/asistencias.js"></script>
</body>
</html>
