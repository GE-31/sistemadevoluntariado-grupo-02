<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.sistemadevoluntariado.model.Usuario" %>
<%@ page import="com.sistemadevoluntariado.model.MovimientoFinanciero" %>
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

    List<MovimientoFinanciero> movimientos = (List<MovimientoFinanciero>) request.getAttribute("movimientos");
    if (movimientos == null) movimientos = java.util.Collections.emptyList();

    request.setAttribute("page", "tesoreria");
%>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Tesorería - Sistema de Voluntariado</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/sidebar.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/tesoreria.css">

    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>

<jsp:include page="/includes/sidebar.jsp" />

<main class="main-content">
    <jsp:include page="/includes/topbar.jsp" />

    <div class="content-body">

        <!-- ========================= HEADER ========================= -->
        <div class="tesoreria-header">
            <div>
                <h1>Tesorería</h1>
                <p>Control de ingresos, gastos y balance financiero</p>
            </div>
            <button class="btn btn-primary" onclick="abrirModal()">
                <i class="fa-solid fa-circle-plus"></i> Nuevo Movimiento
            </button>
        </div>

        <!-- ========================= TARJETAS BALANCE ========================= -->
        <div class="balance-cards">
            <div class="balance-card ingreso">
                <div class="balance-icon"><i class="fa-solid fa-arrow-trend-up"></i></div>
                <div class="balance-info">
                    <span class="balance-label">Total Ingresos</span>
                    <span class="balance-monto" id="totalIngresos">S/ 0.00</span>
                </div>
            </div>
            <div class="balance-card gasto">
                <div class="balance-icon"><i class="fa-solid fa-arrow-trend-down"></i></div>
                <div class="balance-info">
                    <span class="balance-label">Total Gastos</span>
                    <span class="balance-monto" id="totalGastos">S/ 0.00</span>
                </div>
            </div>
            <div class="balance-card saldo">
                <div class="balance-icon"><i class="fa-solid fa-wallet"></i></div>
                <div class="balance-info">
                    <span class="balance-label">Saldo Disponible</span>
                    <span class="balance-monto" id="saldoDisponible">S/ 0.00</span>
                </div>
            </div>
        </div>

        <!-- ========================= FILTROS ========================= -->
        <div class="filtros-bar">
            <div class="filtro-grupo">
                <label>Tipo</label>
                <select id="filtroTipo" onchange="filtrarMovimientos()">
                    <option value="">Todos</option>
                    <option value="INGRESO">Ingresos</option>
                    <option value="GASTO">Gastos</option>
                </select>
            </div>
            <div class="filtro-grupo">
                <label>Categoría</label>
                <select id="filtroCategoria" onchange="filtrarMovimientos()">
                    <option value="">Todas</option>
                    <option value="Donaciones">Donaciones</option>
                    <option value="Cuotas">Cuotas</option>
                    <option value="Subvenciones">Subvenciones</option>
                    <option value="Eventos">Eventos</option>
                    <option value="Materiales">Materiales</option>
                    <option value="Transporte">Transporte</option>
                    <option value="Alimentación">Alimentación</option>
                    <option value="Servicios">Servicios</option>
                    <option value="Personal">Personal</option>
                    <option value="Otros">Otros</option>
                </select>
            </div>
            <div class="filtro-grupo">
                <label>Desde</label>
                <input type="date" id="filtroFechaIni" onchange="filtrarMovimientos()">
            </div>
            <div class="filtro-grupo">
                <label>Hasta</label>
                <input type="date" id="filtroFechaFin" onchange="filtrarMovimientos()">
            </div>
            <button class="btn btn-secondary btn-sm" onclick="limpiarFiltros()">
                <i class="fa-solid fa-eraser"></i> Limpiar
            </button>
        </div>

        <!-- ========================= TABLA MOVIMIENTOS ========================= -->
        <div class="tesoreria-table">
            <table>
                <thead>
                    <tr>
                        <th>Tipo</th>
                        <th>Monto</th>
                        <th>Descripción</th>
                        <th>Categoría</th>
                        <th>Comprobante</th>
                        <th>Fecha</th>
                        <th>Actividad</th>
                        <th>Registrado por</th>
                        <th>Acciones</th>
                    </tr>
                </thead>
                <tbody id="tbodyMovimientos">
                <% if (!movimientos.isEmpty()) {
                    for (MovimientoFinanciero m : movimientos) { %>
                    <tr>
                        <td>
                            <span class="tag <%= m.getTipo().equals("INGRESO") ? "tag-ingreso" : "tag-gasto" %>">
                                <%= m.getTipo() %>
                            </span>
                        </td>
                        <td><strong>S/ <%= String.format("%.2f", m.getMonto()) %></strong></td>
                        <td><%= m.getDescripcion() %></td>
                        <td><%= m.getCategoria() %></td>
                        <td><%= m.getComprobante() != null ? m.getComprobante() : "—" %></td>
                        <td><%= m.getFechaMovimiento() %></td>
                        <td><%= m.getActividad() %></td>
                        <td><%= m.getUsuarioRegistro() %></td>
                        <td class="acciones-cell">
                            <button class="btn-icon edit" onclick="editarMovimiento(<%= m.getIdMovimiento() %>)" title="Editar">✎</button>
                            <button class="btn-icon delete" onclick="eliminarMovimiento(<%= m.getIdMovimiento() %>)" title="Eliminar">🗑</button>
                        </td>
                    </tr>
                <% }} else { %>
                    <tr><td colspan="9" class="no-data">No hay movimientos registrados</td></tr>
                <% } %>
                </tbody>
            </table>
        </div>

        <!-- ========================= GRÁFICOS ========================= -->
        <div class="graficos-container">
            <div class="grafico-card">
                <h3><i class="fa-solid fa-chart-line"></i> Tendencia Mensual</h3>
                <canvas id="chartMensual"></canvas>
            </div>
            <div class="grafico-card">
                <h3><i class="fa-solid fa-chart-pie"></i> Distribución por Categoría</h3>
                <canvas id="chartCategoria"></canvas>
            </div>
        </div>

    </div>
</main>

<!-- ========================= MODAL MOVIMIENTO ========================= -->
<div id="modalMovimiento" class="modal-overlay">
    <div class="modal-container">
        <div class="modal-card">

            <div class="modal-header-enhanced">
                <div>
                    <h2 id="tituloModal">Registrar Movimiento</h2>
                    <p>Ingresa la información del movimiento financiero</p>
                </div>
                <button class="modal-close-btn" onclick="cerrarModal()">✕</button>
            </div>

            <form id="formMovimiento" onsubmit="guardarMovimiento(event)">
                <input type="hidden" id="idMovimiento" name="idMovimiento">

                <div class="form-grid">

                    <div class="form-group">
                        <label>Tipo *</label>
                        <select id="tipo" name="tipo" class="form-input" required>
                            <option value="">Seleccione</option>
                            <option value="INGRESO">Ingreso</option>
                            <option value="GASTO">Gasto</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>Monto (S/) *</label>
                        <input type="number" id="monto" name="monto" class="form-input" step="0.01" min="0.01" required>
                    </div>

                    <div class="form-group">
                        <label>Categoría *</label>
                        <select id="categoria" name="categoria" class="form-input" required>
                            <option value="">Seleccione</option>
                            <option value="Donaciones">Donaciones</option>
                            <option value="Cuotas">Cuotas</option>
                            <option value="Subvenciones">Subvenciones</option>
                            <option value="Eventos">Eventos</option>
                            <option value="Materiales">Materiales</option>
                            <option value="Transporte">Transporte</option>
                            <option value="Alimentación">Alimentación</option>
                            <option value="Servicios">Servicios</option>
                            <option value="Personal">Personal</option>
                            <option value="Otros">Otros</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label>Fecha *</label>
                        <input type="date" id="fechaMovimiento" name="fechaMovimiento" class="form-input" required>
                    </div>

                    <div class="form-group full-width">
                        <label>Descripción *</label>
                        <textarea id="descripcion" name="descripcion" class="form-textarea" rows="2" required></textarea>
                    </div>

                    <div class="form-group">
                        <label>Nº Comprobante</label>
                        <input type="text" id="comprobante" name="comprobante" class="form-input" placeholder="Opcional">
                    </div>

                    <div class="form-group">
                        <label>Actividad relacionada</label>
                        <select id="idActividad" name="idActividad" class="form-input">
                            <option value="0">Ninguna</option>
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

<script src="${pageContext.request.contextPath}/js/tesoreria.js"></script>

</body>
</html>
