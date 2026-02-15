<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.sistemadevoluntariado.model.Usuario" %>
<%@ page import="com.sistemadevoluntariado.model.Donacion" %>
<%@ page import="java.util.List" %>

<%
    HttpSession ses = request.getSession(false);
    Usuario usuario = (Usuario) (request.getAttribute("usuario") != null
            ? request.getAttribute("usuario")
            : (ses != null ? ses.getAttribute("usuarioLogeado") : null));

    if (usuario == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }

    List<Donacion> donaciones = (List<Donacion>) request.getAttribute("donaciones");
    if (donaciones == null) {
        donaciones = java.util.Collections.emptyList();
    }

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
        <div class="donaciones-header">
            <div>
                <h1>Gestion de Donaciones</h1>
                <p>Registra aportes externos: dinero o bienes en especie</p>
            </div>
            <button class="btn btn-primary" onclick="abrirModal()">
                <i class="fa-solid fa-circle-plus"></i> Registrar Donacion
            </button>
        </div>

        <div class="donaciones-filtros">
            <label for="buscarDonaciones">Buscar</label>
            <input type="text" id="buscarDonaciones" placeholder="Tipo, descripcion, donante, actividad o registrado por">
        </div>

        <div class="donaciones-table">
            <table>
                <thead>
                    <tr>
                        <th>Tipo</th>
                        <th>Cantidad</th>
                        <th>Descripcion</th>
                        <th>Donante</th>
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
                            <span class="tag <%= "DINERO".equals(d.getTipoDonacion()) ? "dinero" : "objeto" %>">
                                <%= "DINERO".equals(d.getTipoDonacion()) ? "DINERO" : "EN ESPECIE" %>
                            </span>
                        </td>
                        <td>
                            <% if ("DINERO".equals(d.getTipoDonacion())) { %>
                                <strong>S/ <%= d.getCantidad() %></strong>
                            <% } else { %>
                                <strong><%= d.getCantidadItem() != null ? d.getCantidadItem() : d.getCantidad() %> <%= d.getItemUnidadMedida() != null ? d.getItemUnidadMedida() : "unidad" %></strong>
                                <div class="item-obs"><%= d.getItemNombre() != null ? d.getItemNombre() : "Item no especificado" %></div>
                            <% } %>
                        </td>
                        <td><%= d.getDescripcion() %></td>
                        <td><%= d.getDonanteNombre() != null ? d.getDonanteNombre() : "ANONIMO" %></td>
                        <td><%= d.getActividad() %></td>
                        <td><%= d.getUsuarioRegistro() %></td>
                        <td><%= d.getRegistradoEn() %></td>
                        <td class="acciones-cell">
                            <button class="btn-icon edit" onclick="editarDonacion(<%= d.getIdDonacion() %>)" title="Editar">
                                <i class="fas fa-edit"></i>
                            </button>
                            <button class="btn-icon delete" onclick="anularDonacion(<%= d.getIdDonacion() %>)" title="Anular">
                                <i class="fas fa-trash"></i>
                            </button>
                        </td>
                    </tr>
                <% }} else { %>
                    <tr>
                        <td colspan="8" class="no-data">No hay donaciones registradas</td>
                    </tr>
                <% } %>
                </tbody>
            </table>
        </div>
        <div id="donacionesPaginacion" class="table-pagination" style="display:none;">
            <button type="button" class="btn btn-secondary btn-sm" id="btnPaginaAnterior">Anterior</button>
            <span id="textoPaginacionDonaciones"></span>
            <button type="button" class="btn btn-secondary btn-sm" id="btnPaginaSiguiente">Siguiente</button>
        </div>
    </div>
</main>

<div id="modalDonacion" class="modal-overlay">
    <div class="modal-container">
        <div class="modal-card">
            <div class="modal-header-enhanced">
                <div>
                    <h2 id="tituloModal">Registrar Donacion</h2>
                    <p>Ingresa la informacion de la donacion</p>
                </div>
                <button class="modal-close-btn" onclick="cerrarModal()">x</button>
            </div>

            <form id="formDonacion" action="${pageContext.request.contextPath}/donaciones" method="POST">
                <input type="hidden" id="idDonacion" name="idDonacion">
                <input type="hidden" id="accionDonacion" name="accion" value="registrar">
                <input type="hidden" id="motivoEdicion" name="motivoEdicion">

                <div class="form-grid">
                    <div class="form-group">
                        <label>Tipo de Donacion *</label>
                        <select id="tipoDonacion" name="tipoDonacion" class="form-input" required onchange="onTipoDonacionChange()">
                            <option value="">Seleccione</option>
                            <option value="1">Dinero</option>
                            <option value="2">En especie (item para inventario)</option>
                        </select>
                    </div>

                    <div class="form-group">
                        <label id="labelCantidad">Cantidad *</label>
                        <input type="number" id="cantidad" name="cantidad" class="form-input" min="0.01" step="0.01" required>
                    </div>

                    <div class="form-group full-width">
                        <label>Descripcion *</label>
                        <textarea id="descripcion" name="descripcion" class="form-textarea" required></textarea>
                    </div>

                    <section class="form-group full-width donation-mode-box">
                        <div class="inventory-section-title">
                            <i class="fa-solid fa-user"></i>
                            <span>Donante</span>
                        </div>
                        <div class="form-grid">
                            <div class="form-group full-width">
                                <label>
                                    <input type="checkbox" id="donacionAnonima" name="donacionAnonima" value="1">
                                    Donacion anonima
                                </label>
                            </div>
                        </div>
                        <div id="donanteFields" class="form-grid">
                            <div class="form-group">
                                <label>Tipo de donante *</label>
                                <select id="tipoDonante" name="tipoDonante" class="form-input">
                                    <option value="PERSONA">Persona</option>
                                    <option value="EMPRESA">Empresa</option>
                                    <option value="GRUPO">Grupo</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label>Nombre del donante *</label>
                                <input type="text" id="nombreDonante" name="nombreDonante" class="form-input" maxlength="150">
                            </div>
                            <div class="form-group">
                                <label>Correo</label>
                                <input type="email" id="correoDonante" name="correoDonante" class="form-input" maxlength="100">
                            </div>
                            <div class="form-group">
                                <label>Telefono</label>
                                <input type="text" id="telefonoDonante" name="telefonoDonante" class="form-input" maxlength="30">
                            </div>
                        </div>
                    </section>

                    <section id="seccionEspecie" class="form-group full-width donation-mode-box" style="display:none;">
                        <div class="inventory-section-title">
                            <i class="fa-solid fa-boxes-stacked"></i>
                            <span>Impacto en Inventario</span>
                        </div>
                        <p class="inventory-help">
                            Toda donacion en especie genera una entrada de stock en inventario.
                        </p>
                        <div class="form-grid">
                            <div class="form-group full-width">
                                <label>Item de inventario existente *</label>
                                <select id="idItem" name="idItem" class="form-input">
                                    <option value="">Seleccione item existente</option>
                                </select>
                            </div>

                            <div class="form-group full-width">
                                <label>
                                    <input type="checkbox" id="crearNuevoItem" name="crearNuevoItem" value="1">
                                    No encuentro el item, crear nuevo
                                </label>
                            </div>
                        </div>

                        <div id="nuevoItemFields" class="form-grid" style="display:none;">
                            <div class="form-group">
                                <label>Nombre del item *</label>
                                <input type="text" id="itemNombre" name="itemNombre" class="form-input" maxlength="150">
                            </div>
                            <div class="form-group">
                                <label>Categoria *</label>
                                <select id="itemCategoria" class="form-input">
                                    <option value="">Seleccione categoria</option>
                                    <option value="ALIMENTOS">Alimentos</option>
                                    <option value="ROPA">Ropa</option>
                                    <option value="UTILES">Utiles</option>
                                    <option value="MEDICINAS">Medicinas</option>
                                    <option value="HIGIENE">Higiene</option>
                                    <option value="OTROS">Otros</option>
                                    <option value="__OTRO__">Otro (especificar)</option>
                                </select>
                                <input type="text" id="itemCategoriaOtro" class="form-input" maxlength="50" placeholder="Especifique categoria" style="display:none; margin-top:8px;">
                                <input type="hidden" id="itemCategoriaValor" name="itemCategoria">
                            </div>
                            <div class="form-group">
                                <label>Unidad de medida *</label>
                                <select id="itemUnidadMedida" class="form-input">
                                    <option value="">Seleccione unidad</option>
                                    <option value="unidad">Unidad</option>
                                    <option value="kg">Kg</option>
                                    <option value="litro">Litro</option>
                                    <option value="caja">Caja</option>
                                    <option value="paquete">Paquete</option>
                                    <option value="lata">Lata</option>
                                    <option value="botella">Botella</option>
                                    <option value="__OTRO__">Otro (especificar)</option>
                                </select>
                                <input type="text" id="itemUnidadMedidaOtro" class="form-input" maxlength="30" placeholder="Especifique unidad" style="display:none; margin-top:8px;">
                                <input type="hidden" id="itemUnidadMedidaValor" name="itemUnidadMedida">
                            </div>
                            <div class="form-group">
                                <label>Stock minimo sugerido</label>
                                <input type="number" id="itemStockMinimo" name="itemStockMinimo" class="form-input" min="0" step="0.01" value="0">
                            </div>
                        </div>
                    </section>

                    <div class="form-group full-width">
                        <label>Actividad *</label>
                        <select id="actividad" name="actividad" class="form-input" required>
                            <option value="">Seleccione actividad</option>
                        </select>
                    </div>
                </div>

                <div class="modal-actions">
                    <button type="button" class="btn btn-secondary" onclick="cerrarModal()">Cancelar</button>
                    <button type="submit" id="btnGuardarDonacion" class="btn btn-primary">Registrar donacion</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/donaciones.js?v=20260214-4"></script>

</body>
</html>

