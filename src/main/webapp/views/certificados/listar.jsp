<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.sistemadevoluntariado.model.Usuario" %>
<%@ page import="com.sistemadevoluntariado.model.Certificado" %>
<%@ page import="com.sistemadevoluntariado.model.Voluntario" %>
<%@ page import="com.sistemadevoluntariado.model.Actividad" %>
<%@ page import="java.util.List" %>

<%
    HttpSession ses = request.getSession(false);
    Usuario usuario = (Usuario)(request.getAttribute("usuario") != null
        ? request.getAttribute("usuario")
        : (ses != null ? ses.getAttribute("usuarioLogeado") : null));

    if (usuario == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    List<Certificado> certificados = (List<Certificado>) request.getAttribute("certificados");
    List<Voluntario> voluntarios = (List<Voluntario>) request.getAttribute("voluntarios");
    List<Actividad> actividades = (List<Actividad>) request.getAttribute("actividades");
    
    if (certificados == null) certificados = java.util.Collections.emptyList();
    if (voluntarios == null) voluntarios = java.util.Collections.emptyList();
    if (actividades == null) actividades = java.util.Collections.emptyList();

    request.setAttribute("page", "certificados");
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Certificados - Sistema de Voluntariado</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/certificados.css">

    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>

<jsp:include page="/includes/sidebar.jsp" />

<main class="main-content">
    <jsp:include page="/includes/topbar.jsp" />

    <div class="content-body">

        <!-- Header -->
        <div class="certificados-header">
            <div>
                <h1>Gestión de Certificados</h1>
                <p>Emisión y control de certificados de voluntariado</p>
            </div>
            <div class="header-actions">
                <button class="btn btn-secondary" onclick="abrirModalVerificar()">
                    <i class="fa-solid fa-magnifying-glass"></i> Verificar Certificado
                </button>
                <button class="btn btn-primary" onclick="abrirModal()">
                    <i class="fa-solid fa-certificate"></i> Emitir Certificado
                </button>
            </div>
        </div>

        <!-- Estadísticas -->
        <div class="stats-cards">
            <div class="stat-card">
                <div class="stat-icon emitidos">
                    <i class="fa-solid fa-certificate"></i>
                </div>
                <div class="stat-info">
                    <h3 id="totalEmitidos"><%= certificados.stream().filter(c -> "EMITIDO".equals(c.getEstado())).count() %></h3>
                    <p>Certificados Emitidos</p>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon anulados">
                    <i class="fa-solid fa-ban"></i>
                </div>
                <div class="stat-info">
                    <h3 id="totalAnulados"><%= certificados.stream().filter(c -> "ANULADO".equals(c.getEstado())).count() %></h3>
                    <p>Certificados Anulados</p>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon horas">
                    <i class="fa-solid fa-clock"></i>
                </div>
                <div class="stat-info">
                    <h3 id="totalHoras"><%= certificados.stream().mapToInt(Certificado::getHorasVoluntariado).sum() %></h3>
                    <p>Total Horas Certificadas</p>
                </div>
            </div>
        </div>

        <!-- Tabla -->
        <div class="certificados-table">
            <table>
                <thead>
                    <tr>
                        <th>Código</th>
                        <th>Voluntario</th>
                        <th>DNI</th>
                        <th>Actividad</th>
                        <th>Horas</th>
                        <th>Fecha Emisión</th>
                        <th>Estado</th>
                        <th>Acciones</th>
                    </tr>
                </thead>

                <tbody id="tbodyCertificados">
                <% if (!certificados.isEmpty()) { 
                    for (Certificado c : certificados) { %>

                    <tr>
                        <td>
                            <span class="codigo-certificado">
                                <i class="fa-solid fa-qrcode"></i>
                                <%= c.getCodigoCertificado() %>
                            </span>
                        </td>
                        <td><%= c.getNombreVoluntario() %></td>
                        <td><%= c.getDniVoluntario() %></td>
                        <td><%= c.getNombreActividad() %></td>
                        <td><strong><%= c.getHorasVoluntariado() %>h</strong></td>
                        <td><%= c.getFechaEmision() %></td>
                        <td>
                            <span class="estado-badge <%= c.getEstado().toLowerCase() %>">
                                <%= c.getEstado() %>
                            </span>
                        </td>
                        <td class="acciones-cell">
                            <button class="btn-icon view" onclick="verCertificado(<%= c.getIdCertificado() %>)" title="Ver Certificado">
                                <i class="fa-solid fa-eye"></i>
                            </button>
                            <button class="btn-icon print" onclick="imprimirCertificado(<%= c.getIdCertificado() %>)" title="Imprimir">
                                <i class="fa-solid fa-print"></i>
                            </button>
                            <% if ("EMITIDO".equals(c.getEstado())) { %>
                            <button class="btn-icon delete" onclick="anularCertificado(<%= c.getIdCertificado() %>)" title="Anular">
                                <i class="fa-solid fa-ban"></i>
                            </button>
                            <% } %>
                        </td>
                    </tr>

                <% }} else { %>
                    <tr>
                        <td colspan="8" class="no-data">No hay certificados emitidos</td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </div>

    </div>
</main>

<!-- MODAL EMITIR CERTIFICADO -->
<div id="modalCertificado" class="modal-overlay">
    <div class="modal-container">
        <div class="modal-card">

            <div class="modal-header-enhanced">
                <div>
                    <h2 id="tituloModal">Emitir Certificado</h2>
                    <p>Complete los datos para generar el certificado</p>
                </div>

                <button class="modal-close-btn" onclick="cerrarModal()">
                    ✕
                </button>
            </div>

            <form id="formCertificado" onsubmit="guardarCertificado(event)">
                <input type="hidden" name="action" value="crear">
                <div class="form-grid">

                    <div class="form-group full-width">
                        <label>Voluntario *</label>
                        <select id="idVoluntario" name="idVoluntario" class="form-input" required>
                            <option value="">Seleccione un voluntario</option>
                            <% for (Voluntario v : voluntarios) { 
                                if ("ACTIVO".equals(v.getEstado())) { %>
                            <option value="<%= v.getIdVoluntario() %>">
                                <%= v.getNombres() %> <%= v.getApellidos() %> - DNI: <%= v.getDni() %>
                            </option>
                            <% }} %>
                        </select>
                    </div>

                    <div class="form-group full-width">
                        <label>Actividad *</label>
                        <select id="idActividad" name="idActividad" class="form-input" required>
                            <option value="">Seleccione una actividad</option>
                            <% for (Actividad a : actividades) { %>
                            <option value="<%= a.getIdActividad() %>">
                                <%= a.getNombre() %> (<%= a.getFechaInicio() %>)
                            </option>
                            <% } %>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>Horas de Voluntariado *</label>
                        <input type="number" id="horasVoluntariado" name="horasVoluntariado" 
                               class="form-input" min="1" max="500" required placeholder="Ej: 20">
                    </div>

                    <div class="form-group full-width">
                        <label>Observaciones</label>
                        <textarea id="observaciones" name="observaciones" class="form-textarea" 
                                  placeholder="Notas adicionales sobre la participación..."></textarea>
                    </div>

                </div>

                <div class="modal-actions">
                    <button type="button" class="btn btn-secondary" onclick="cerrarModal()">Cancelar</button>
                    <button type="submit" class="btn btn-primary">
                        <i class="fa-solid fa-certificate"></i> Emitir Certificado
                    </button>
                </div>
            </form>

        </div>
    </div>
</div>

<!-- MODAL VERIFICAR CERTIFICADO -->
<div id="modalVerificar" class="modal-overlay">
    <div class="modal-container modal-small">
        <div class="modal-card">

            <div class="modal-header-enhanced">
                <div>
                    <h2>Verificar Certificado</h2>
                    <p>Ingrese el código del certificado</p>
                </div>

                <button class="modal-close-btn" onclick="cerrarModalVerificar()">
                    ✕
                </button>
            </div>

            <div class="verificar-form">
                <div class="form-group">
                    <label>Código del Certificado</label>
                    <input type="text" id="codigoVerificar" class="form-input" 
                           placeholder="Ej: CERT-2026-001" style="text-transform: uppercase;">
                </div>
                <button class="btn btn-primary full-width" onclick="verificarCertificado()">
                    <i class="fa-solid fa-search"></i> Verificar
                </button>
                
                <div id="resultadoVerificacion" class="resultado-verificacion" style="display: none;">
                    <!-- Resultado dinámico -->
                </div>
            </div>

        </div>
    </div>
</div>

<!-- MODAL VER CERTIFICADO -->
<div id="modalVer" class="modal-overlay">
    <div class="modal-container modal-certificado" style="width: 90%; max-width: 1000px; height: 85vh;">
        <div class="modal-card certificado-preview" style="height: 100%; display: flex; flex-direction: column;">
            <button class="modal-close-btn" onclick="cerrarModalVer()">✕</button>
            
            <iframe id="certificadoPDFFrame" src="" style="flex: 1; width: 100%; border: none; border-radius: 8px;"></iframe>
            
            <div class="modal-actions" style="margin-top: 10px;">
                <button class="btn btn-secondary" onclick="cerrarModalVer()">Cerrar</button>
            </div>
        </div>
    </div>
</div>

<script>
    // Pasar el contextPath desde JSP a JavaScript
    const contextPath = '${pageContext.request.contextPath}';
</script>
<script src="${pageContext.request.contextPath}/js/certificados.js"></script>

</body>
</html>
