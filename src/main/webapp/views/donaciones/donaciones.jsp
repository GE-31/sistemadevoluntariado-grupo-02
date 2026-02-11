<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.sistemadevoluntariado.model.Usuario" %>
<%@ page import="com.sistemadevoluntariado.model.Donacion" %>
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

    List<Donacion> donaciones = (List<Donacion>) request.getAttribute("donaciones");
    if (donaciones == null) donaciones = java.util.Collections.emptyList();

    request.setAttribute("page", "donaciones");
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Donaciones - Sistema de Voluntariado</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/donaciones.css">

    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>

<jsp:include page="/includes/sidebar.jsp" />

<main class="main-content">
    <jsp:include page="/includes/topbar.jsp" />

    <div class="content-body">

        <!-- Header -->
        <div class="donaciones-header">
            <div>
                <h1>Gestión de Donaciones</h1>
                <p>Control y registro de aportes monetarios y materiales</p>
            </div>
            <button class="btn btn-primary" onclick="abrirModal()">
                <i class="fa-solid fa-circle-plus"></i> Registrar Donación
            </button>
        </div>

        <!-- Tabla -->
        <div class="donaciones-table">
            <table>
                <thead>
                    <tr>
                        <th>Tipo</th>
                        <th>Cantidad</th>
                        <th>Descripción</th>
                        <th>Actividad</th>
                        <th>Registrado por</th>
                        <th>Fecha</th>
                        <th>Acciones</th>
                    </tr>
                </thead>

                <tbody id="tbodyDonaciones">
                <% if (!donaciones.isEmpty()) { 
                    for (Donacion d : donaciones) { %>

                    <tr>
                        <td>
                            <span class="tag <%= d.getTipoDonacion().equals("DINERO") ? "dinero" : "objeto" %>">
                                <%= d.getTipoDonacion() %>
                            </span>
                        </td>

                        <td>
                            <% if (d.getTipoDonacion().equals("DINERO")) { %>
                                <strong>S/ <%= d.getCantidad() %></strong>
                            <% } else { %>
                                <%= d.getCantidad() %> unidades
                            <% } %>
                        </td>

                        <td><%= d.getDescripcion() %></td>
                        <td><%= d.getActividad() %></td>
                        <td><%= d.getUsuarioRegistro() %></td>
                        <td><%= d.getRegistradoEn() %></td>

                        <td class="acciones-cell">
                            <button class="btn-icon edit" onclick="editar(<%= d.getIdDonacion() %>)" title="Editar">
                                ✎
                            </button>
                            <button class="btn-icon delete" onclick="eliminar(<%= d.getIdDonacion() %>)" title="Eliminar">
                                🗑
                            </button>
                        </td>
                    </tr>

                <% }} else { %>
                    <tr>
                        <td colspan="7" class="no-data">No hay donaciones registradas</td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </div>

    </div>
</main>

<!-- MODAL DONACIONES -->
<div id="modalDonacion" class="modal-overlay">
    <div class="modal-container">
        <div class="modal-card">

            <div class="modal-header-enhanced">
                <div>
                    <h2 id="tituloModal">Registrar Donación</h2>
                    <p>Ingresa la información de la donación</p>
                </div>

                <button class="modal-close-btn" onclick="cerrarModal()">
                    ✕
                </button>
            </div>

            <form id="formDonacion" action="${pageContext.request.contextPath}/donaciones" method="POST">
                <input type="hidden" id="idDonacion" name="idDonacion">

                <div class="form-grid">

                    <div class="form-group">
                        <label>Tipo de Donación *</label>
                        <select id="tipoDonacion" name="tipoDonacion" class="form-input" required>
                            <option value="">Seleccione</option>
                            <option value="1">Dinero</option>
                            <option value="2">Objeto</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>Cantidad *</label>
                        <input type="number" id="cantidad" name="cantidad" class="form-input" min="1" required>
                    </div>

                    <div class="form-group full-width">
                        <label>Descripción *</label>
                        <textarea id="descripcion" name="descripcion" class="form-textarea" required></textarea>
                    </div>

                    <div class="form-group full-width">
                        <label>Actividad *</label>
                        <select id="actividad" name="actividad" class="form-input" required>
                            <option value="">Seleccione actividad</option>
                            <%-- Llenado dinámico desde JS --%>
                        </select>
                    </div>

                </div>

                <div class="modal-actions">
                    <button type="button" class="btn btn-secondary" onclick="cerrarModal()">Cancelar</button>
                    <button type="submit" class="btn btn-primary">Guardar</button>
                </div>
            </form>

        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/donaciones.js"></script>

</body>
</html>
