<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.sistemadevoluntariado.model.Usuario" %>
<%@ page import="com.sistemadevoluntariado.model.InventarioItem" %>
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

    List<InventarioItem> items = (List<InventarioItem>) request.getAttribute("items");
    if (items == null) items = java.util.Collections.emptyList();

    Integer stockBajoTotal = (Integer) request.getAttribute("stockBajoTotal");
    if (stockBajoTotal == null) stockBajoTotal = 0;

    request.setAttribute("page", "inventario");
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Inventario - Sistema de Voluntariado</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/inventario.css">

    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>

<jsp:include page="/includes/sidebar.jsp" />

<main class="main-content">
    <jsp:include page="/includes/topbar.jsp" />

    <div class="content-body">

        <div class="inventario-header">
            <div>
                <h1>Inventario</h1>
                <p>Gestiona catalogo, stock minimo y movimientos de entrada/salida</p>
            </div>
            <button class="btn btn-primary" onclick="abrirModal()">
                <i class="fa-solid fa-circle-plus"></i> Nuevo Item Base
            </button>
        </div>

        <div class="inventario-kpis">
            <div class="kpi-card warning">
                <div class="kpi-icon"><i class="fa-solid fa-triangle-exclamation"></i></div>
                <div class="kpi-text">
                    <span class="kpi-label">Stock bajo</span>
                    <span class="kpi-value" id="stockBajoTotal"><%= stockBajoTotal %></span>
                </div>
            </div>
        </div>

        <div class="filtros-bar">
            <div class="filtro-grupo">
                <label>Buscar</label>
                <input type="text" id="filtroQ" placeholder="Nombre u observación">
            </div>
            <div class="filtro-grupo">
                <label>Categoría</label>
                <select id="filtroCategoria">
                    <option value="">Todas</option>
                    <option value="ALIMENTOS">Alimentos</option>
                    <option value="ROPA">Ropa</option>
                    <option value="UTILES">Útiles</option>
                    <option value="MEDICINAS">Medicinas</option>
                    <option value="HIGIENE">Higiene</option>
                    <option value="OTROS">Otros</option>
                </select>
            </div>
            <div class="filtro-grupo">
                <label>Estado</label>
                <select id="filtroEstado">
                    <option value="">Todos</option>
                    <option value="ACTIVO">Activo</option>
                    <option value="INACTIVO">Inactivo</option>
                </select>
            </div>
            <div class="filtro-check">
                <label><input type="checkbox" id="filtroStockBajo"> Solo stock bajo</label>
            </div>
            <button class="btn btn-secondary btn-sm" onclick="limpiarFiltros()">
                <i class="fa-solid fa-eraser"></i> Limpiar
            </button>
        </div>

        <div class="inventario-table">
            <table>
                <thead>
                    <tr>
                        <th>Item</th>
                        <th>Categoría</th>
                        <th>Unidad</th>
                        <th>Stock actual</th>
                        <th>Stock mínimo</th>
                        <th>Estado</th>
                        <th>Actualizado</th>
                        <th>Acciones</th>
                    </tr>
                </thead>
                <tbody id="tbodyInventario">
                <% if (!items.isEmpty()) {
                    for (InventarioItem item : items) { %>
                    <tr>
                        <td>
                            <strong><%= item.getNombre() %></strong>
                            <div class="item-obs"><%= item.getObservacion() != null ? item.getObservacion() : "" %></div>
                        </td>
                        <td><%= item.getCategoria() %></td>
                        <td><%= item.getUnidadMedida() %></td>
                        <td>
                            <span class="<%= item.getStockActual() <= item.getStockMinimo() ? "stock-bajo" : "stock-ok" %>">
                                <%= item.getStockActual() %>
                            </span>
                        </td>
                        <td><%= item.getStockMinimo() %></td>
                        <td>
                            <span class="tag <%= "ACTIVO".equalsIgnoreCase(item.getEstado()) ? "tag-activo" : "tag-inactivo" %>">
                                <%= item.getEstado() %>
                            </span>
                        </td>
                        <td><%= item.getActualizadoEn() != null ? item.getActualizadoEn() : item.getCreadoEn() %></td>
                        <td class="acciones-cell">
                            <button class="btn-icon edit" onclick="abrirModalMovimiento(<%= item.getIdItem() %>)" title="Registrar movimiento">
                                <i class="fa-solid fa-right-left"></i>
                            </button>
                            <button class="btn-icon edit" onclick="editarItem(<%= item.getIdItem() %>)" title="Editar">&#9998;</button>
                            <% if ("ACTIVO".equalsIgnoreCase(item.getEstado())) { %>
                                <button class="btn-icon disable" onclick="cambiarEstado(<%= item.getIdItem() %>, 'INACTIVO')" title="Desactivar">&#8856;</button>
                            <% } else { %>
                                <button class="btn-icon enable" onclick="cambiarEstado(<%= item.getIdItem() %>, 'ACTIVO')" title="Activar">&#10003;</button>
                            <% } %>
                        </td>
                    </tr>
                <% }} else { %>
                    <tr><td colspan="8" class="no-data">No hay items registrados</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
        <div id="inventarioPaginacion" class="table-pagination" style="display:none;">
            <button type="button" class="btn btn-secondary btn-sm" id="btnInventarioAnterior">Anterior</button>
            <span id="textoPaginacionInventario"></span>
            <button type="button" class="btn btn-secondary btn-sm" id="btnInventarioSiguiente">Siguiente</button>
        </div>
    </div>
</main>

<div id="modalInventario" class="modal-overlay">
    <div class="modal-container">
        <div class="modal-card">
            <div class="modal-header-enhanced">
                <div>
                    <h2 id="tituloModal">Nuevo Item</h2>
                    <p>Registrar o actualizar datos base del item</p>
                </div>
                <button class="modal-close-btn" onclick="cerrarModal()">&#10005;</button>
            </div>

            <form id="formInventario" onsubmit="guardarItem(event)">
                <input type="hidden" id="idItem" name="idItem">

                <div class="form-grid">
                    <div class="form-group">
                        <label>Nombre *</label>
                        <input type="text" id="nombre" name="nombre" class="form-input" required>
                    </div>
                    <div class="form-group">
                        <label>Categoría *</label>
                        <select id="categoria" name="categoria" class="form-input" required>
                            <option value="">Seleccione</option>
                            <option value="ALIMENTOS">Alimentos</option>
                            <option value="ROPA">Ropa</option>
                            <option value="UTILES">Útiles</option>
                            <option value="MEDICINAS">Medicinas</option>
                            <option value="HIGIENE">Higiene</option>
                            <option value="OTROS">Otros</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Unidad *</label>
                        <select id="unidadMedida" name="unidadMedida" class="form-input" required>
                            <option value="">Seleccione</option>
                            <option value="unidad">Unidad</option>
                            <option value="kg">Kg</option>
                            <option value="litro">Litro</option>
                            <option value="caja">Caja</option>
                            <option value="paquete">Paquete</option>
                            <option value="lata">Lata</option>
                            <option value="botella">Botella</option>
                            <option value="__OTRO__">Otro (especificar)</option>
                        </select>
                        <input type="text" id="unidadMedidaOtro" class="form-input" maxlength="30" placeholder="Especifique unidad" style="display:none; margin-top:8px;">
                    </div>
                    <div class="form-group">
                        <label>Stock mínimo *</label>
                        <input type="number" id="stockMinimo" name="stockMinimo" step="0.01" min="0" class="form-input" required>
                    </div>
                    <div class="form-group full-width">
                        <small>El stock actual se modifica solo con movimientos (donaciones, entradas o salidas).</small>
                    </div>
                    <div class="form-group full-width">
                        <label>Observación</label>
                        <textarea id="observacion" name="observacion" class="form-textarea" rows="2"></textarea>
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

<div id="modalMovimiento" class="modal-overlay">
    <div class="modal-container">
        <div class="modal-card">
            <div class="modal-header-enhanced">
                <div>
                    <h2>Registrar Movimiento</h2>
                    <p>Ajusta el stock con trazabilidad</p>
                </div>
                <button class="modal-close-btn" onclick="cerrarModalMovimiento()">&#10005;</button>
            </div>

            <form id="formMovimiento" onsubmit="guardarMovimiento(event)">
                <input type="hidden" id="movIdItem" name="movIdItem">

                <div class="form-grid">
                    <div class="form-group full-width">
                        <label>Item</label>
                        <input type="text" id="movNombreItem" class="form-input" readonly>
                    </div>
                    <div class="form-group">
                        <label>Stock actual</label>
                        <input type="text" id="movStockActual" class="form-input" readonly>
                    </div>
                    <div class="form-group">
                        <label>Tipo de movimiento *</label>
                        <select id="movTipo" class="form-input" required>
                            <option value="">Seleccione</option>
                            <option value="ENTRADA">Entrada</option>
                            <option value="SALIDA">Salida</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Cantidad *</label>
                        <input type="number" id="movCantidad" class="form-input" min="0.01" step="0.01" required>
                    </div>
                    <div class="form-group">
                        <label>Motivo *</label>
                        <input type="text" id="movMotivo" class="form-input" maxlength="30" placeholder="COMPRA, CONSUMO, MERMA..." required>
                    </div>
                    <div class="form-group full-width">
                        <label>Observación</label>
                        <textarea id="movObservacion" class="form-textarea" rows="2"></textarea>
                    </div>
                </div>

                <div class="modal-actions">
                    <button type="button" class="btn btn-secondary" onclick="cerrarModalMovimiento()">Cancelar</button>
                    <button type="submit" class="btn btn-primary">Registrar movimiento</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/inventario.js?v=20260214-2"></script>
</body>
</html>
