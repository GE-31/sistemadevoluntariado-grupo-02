const modalInventario = document.getElementById("modalInventario");
const formInventario = document.getElementById("formInventario");
const modalMovimiento = document.getElementById("modalMovimiento");
const PAGINA_TAMANO_INVENTARIO = 5;
let paginaActualInventario = 1;
let totalPaginasInventario = 1;
let inventarioFiltradoActual = [];
let editingIdInventario = null;

document.addEventListener("DOMContentLoaded", () => {
    configurarFiltros();
    configurarPaginacionInventario();
    cargarStockBajo();
    configurarUnidadMedida();
    filtrarInventario();
});

function configurarFiltros() {
    const q = document.getElementById("filtroQ");
    const categoria = document.getElementById("filtroCategoria");
    const estado = document.getElementById("filtroEstado");
    const stockBajo = document.getElementById("filtroStockBajo");

    if (q) q.addEventListener("input", filtrarInventario);
    if (categoria) categoria.addEventListener("change", filtrarInventario);
    if (estado) estado.addEventListener("change", filtrarInventario);
    if (stockBajo) stockBajo.addEventListener("change", filtrarInventario);
}

function abrirModal() {
    editingIdInventario = null;
    formInventario.reset();
    document.getElementById("idItem").value = "";
    document.getElementById("tituloModal").textContent = "Nuevo Item";
    mostrarUnidadOtro(false);
    modalInventario.style.display = "flex";
}

function cerrarModal() {
    modalInventario.style.display = "none";
}

async function guardarItem(event) {
    event.preventDefault();

    const params = new URLSearchParams();
    params.append("accion", editingIdInventario ? "actualizar" : "registrar");
    if (editingIdInventario) {
        params.append("idItem", editingIdInventario);
    }
    params.append("nombre", document.getElementById("nombre").value);
    params.append("categoria", document.getElementById("categoria").value);
    params.append("unidadMedida", resolverUnidadMedida());
    params.append("stockMinimo", document.getElementById("stockMinimo").value);
    params.append("observacion", document.getElementById("observacion").value);

    try {
        const resp = await fetch("inventario", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: params.toString()
        });
        const result = await resp.json();
        if (result.ok) {
            cerrarModal();
            await filtrarInventario();
            await cargarStockBajo();
        } else {
            alert(result.message || "No se pudo guardar el item");
        }
    } catch (e) {
        console.error("Error guardando item:", e);
        alert("Error al guardar el item");
    }
}

async function editarItem(id) {
    try {
        const resp = await fetch("inventario?accion=obtener&id=" + id);
        const item = await resp.json();
        if (!item) return;

        editingIdInventario = id;
        document.getElementById("idItem").value = item.idItem;
        document.getElementById("nombre").value = item.nombre || "";
        document.getElementById("categoria").value = item.categoria || "";
        asignarUnidadMedida(item.unidadMedida || "");
        document.getElementById("stockMinimo").value = item.stockMinimo ?? 0;
        document.getElementById("observacion").value = item.observacion || "";
        document.getElementById("tituloModal").textContent = "Editar Item";
        modalInventario.style.display = "flex";
    } catch (e) {
        console.error("Error obteniendo item:", e);
    }
}

async function cambiarEstado(idItem, estado) {
    if (!confirm("Confirmar cambio de estado a " + estado + "?")) return;

    const params = new URLSearchParams();
    params.append("accion", "cambiar_estado");
    params.append("idItem", idItem);
    params.append("estado", estado);

    try {
        const resp = await fetch("inventario", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: params.toString()
        });
        const result = await resp.json();
        if (result.ok) {
            await filtrarInventario();
            await cargarStockBajo();
        } else {
            alert(result.message || "No se pudo actualizar estado");
        }
    } catch (e) {
        console.error("Error cambiando estado:", e);
    }
}

async function abrirModalMovimiento(idItem) {
    try {
        const resp = await fetch("inventario?accion=obtener&id=" + idItem);
        const item = await resp.json();
        if (!item) return;

        document.getElementById("movIdItem").value = item.idItem;
        document.getElementById("movNombreItem").value = `${item.nombre || ""} (${item.unidadMedida || "unidad"})`;
        document.getElementById("movStockActual").value = item.stockActual ?? 0;
        document.getElementById("movTipo").value = "";
        document.getElementById("movCantidad").value = "";
        document.getElementById("movMotivo").value = "";
        document.getElementById("movObservacion").value = "";
        modalMovimiento.style.display = "flex";
    } catch (e) {
        console.error("Error al abrir modal de movimiento:", e);
    }
}

function cerrarModalMovimiento() {
    modalMovimiento.style.display = "none";
}

async function guardarMovimiento(event) {
    event.preventDefault();

    const params = new URLSearchParams();
    params.append("accion", "registrar_movimiento");
    params.append("idItem", document.getElementById("movIdItem").value);
    params.append("tipoMovimiento", document.getElementById("movTipo").value);
    params.append("cantidad", document.getElementById("movCantidad").value);
    params.append("motivo", document.getElementById("movMotivo").value);
    params.append("observacion", document.getElementById("movObservacion").value);

    try {
        const resp = await fetch("inventario", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: params.toString()
        });
        const result = await resp.json();
        if (result.ok) {
            cerrarModalMovimiento();
            await filtrarInventario();
            await cargarStockBajo();
        } else {
            alert(result.message || "No se pudo registrar el movimiento");
        }
    } catch (e) {
        console.error("Error registrando movimiento:", e);
        alert("Error al registrar el movimiento");
    }
}

async function filtrarInventario() {
    paginaActualInventario = 1;
    const q = document.getElementById("filtroQ")?.value || "";
    const categoria = document.getElementById("filtroCategoria")?.value || "";
    const estado = document.getElementById("filtroEstado")?.value || "";
    const stockBajo = document.getElementById("filtroStockBajo")?.checked ? "1" : "";

    const params = new URLSearchParams();
    params.append("accion", "filtrar");
    if (q) params.append("q", q);
    if (categoria) params.append("categoria", categoria);
    if (estado) params.append("estado", estado);
    if (stockBajo) params.append("stockBajo", stockBajo);

    try {
        const resp = await fetch("inventario?" + params.toString());
        const data = await resp.json();
        renderTablaInventario(data);
    } catch (e) {
        console.error("Error filtrando inventario:", e);
    }
}

function limpiarFiltros() {
    document.getElementById("filtroQ").value = "";
    document.getElementById("filtroCategoria").value = "";
    document.getElementById("filtroEstado").value = "";
    document.getElementById("filtroStockBajo").checked = false;
    filtrarInventario();
}

function renderTablaInventario(data) {
    const tbody = document.getElementById("tbodyInventario");
    if (!tbody) return;

    inventarioFiltradoActual = Array.isArray(data) ? data : [];

    if (inventarioFiltradoActual.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="no-data">No hay items que coincidan con los filtros</td></tr>';
        actualizarPaginacionInventario();
        return;
    }

    totalPaginasInventario = Math.max(1, Math.ceil(inventarioFiltradoActual.length / PAGINA_TAMANO_INVENTARIO));
    if (paginaActualInventario > totalPaginasInventario) {
        paginaActualInventario = totalPaginasInventario;
    }
    if (paginaActualInventario < 1) {
        paginaActualInventario = 1;
    }

    const inicio = (paginaActualInventario - 1) * PAGINA_TAMANO_INVENTARIO;
    const fin = inicio + PAGINA_TAMANO_INVENTARIO;
    const paginaData = inventarioFiltradoActual.slice(inicio, fin);

    tbody.innerHTML = paginaData.map((item) => {
        const stockActual = parseFloat(item.stockActual || 0);
        const stockMinimo = parseFloat(item.stockMinimo || 0);
        const stockClass = stockActual <= stockMinimo ? "stock-bajo" : "stock-ok";
        const tagEstado = String(item.estado || "").toUpperCase() === "ACTIVO" ? "tag-activo" : "tag-inactivo";
        const actualizacion = item.actualizadoEn || item.creadoEn || "";

        const btnEstado = String(item.estado || "").toUpperCase() === "ACTIVO"
            ? `<button class="btn-icon disable" onclick="cambiarEstado(${item.idItem}, 'INACTIVO')" title="Desactivar">&#8856;</button>`
            : `<button class="btn-icon enable" onclick="cambiarEstado(${item.idItem}, 'ACTIVO')" title="Activar">&#10003;</button>`;

        return `
            <tr>
                <td>
                    <strong>${escapeHtml(item.nombre || "")}</strong>
                    <div class="item-obs">${escapeHtml(item.observacion || "")}</div>
                </td>
                <td>${escapeHtml(item.categoria || "")}</td>
                <td>${escapeHtml(item.unidadMedida || "")}</td>
                <td><span class="${stockClass}">${stockActual}</span></td>
                <td>${stockMinimo}</td>
                <td><span class="tag ${tagEstado}">${escapeHtml(item.estado || "")}</span></td>
                <td>${escapeHtml(actualizacion)}</td>
                <td class="acciones-cell">
                    <button class="btn-icon edit" onclick="abrirModalMovimiento(${item.idItem})" title="Registrar movimiento">
                        <i class="fa-solid fa-right-left"></i>
                    </button>
                    <button class="btn-icon edit" onclick="editarItem(${item.idItem})" title="Editar">&#9998;</button>
                    ${btnEstado}
                </td>
            </tr>
        `;
    }).join("");

    actualizarPaginacionInventario();
}

async function cargarStockBajo() {
    try {
        const resp = await fetch("inventario?accion=stock_bajo");
        const data = await resp.json();
        const el = document.getElementById("stockBajoTotal");
        if (el) el.textContent = data.total ?? 0;
    } catch (e) {
        console.error("Error cargando stock bajo:", e);
    }
}

function escapeHtml(texto) {
    return String(texto)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function configurarUnidadMedida() {
    const select = document.getElementById("unidadMedida");
    if (!select) return;
    select.addEventListener("change", () => {
        mostrarUnidadOtro(select.value === "__OTRO__");
    });
}

function mostrarUnidadOtro(mostrar) {
    const inputOtro = document.getElementById("unidadMedidaOtro");
    if (!inputOtro) return;
    inputOtro.style.display = mostrar ? "block" : "none";
    inputOtro.required = mostrar;
    if (!mostrar) {
        inputOtro.value = "";
    }
}

function resolverUnidadMedida() {
    const select = document.getElementById("unidadMedida");
    const inputOtro = document.getElementById("unidadMedidaOtro");
    if (!select) return "";
    if (select.value === "__OTRO__") {
        return (inputOtro?.value || "").trim().toLowerCase();
    }
    return (select.value || "").trim().toLowerCase();
}

function asignarUnidadMedida(valor) {
    const select = document.getElementById("unidadMedida");
    const inputOtro = document.getElementById("unidadMedidaOtro");
    if (!select) return;

    const unidad = (valor || "").trim().toLowerCase();
    const opciones = Array.from(select.options).map((opt) => opt.value);
    if (opciones.includes(unidad)) {
        select.value = unidad;
        mostrarUnidadOtro(false);
    } else {
        select.value = "__OTRO__";
        mostrarUnidadOtro(true);
        if (inputOtro) {
            inputOtro.value = unidad;
        }
    }
}

function configurarPaginacionInventario() {
    const btnAnterior = document.getElementById("btnInventarioAnterior");
    const btnSiguiente = document.getElementById("btnInventarioSiguiente");
    if (!btnAnterior || !btnSiguiente) return;

    btnAnterior.addEventListener("click", () => {
        if (paginaActualInventario > 1) {
            paginaActualInventario -= 1;
            renderTablaInventario(inventarioFiltradoActual);
        }
    });

    btnSiguiente.addEventListener("click", () => {
        if (paginaActualInventario < totalPaginasInventario) {
            paginaActualInventario += 1;
            renderTablaInventario(inventarioFiltradoActual);
        }
    });
}

function actualizarPaginacionInventario() {
    const contenedor = document.getElementById("inventarioPaginacion");
    const btnAnterior = document.getElementById("btnInventarioAnterior");
    const btnSiguiente = document.getElementById("btnInventarioSiguiente");
    const texto = document.getElementById("textoPaginacionInventario");
    if (!contenedor || !btnAnterior || !btnSiguiente || !texto) return;

    if (!inventarioFiltradoActual || inventarioFiltradoActual.length === 0) {
        contenedor.style.display = "none";
        return;
    }

    totalPaginasInventario = Math.max(1, Math.ceil(inventarioFiltradoActual.length / PAGINA_TAMANO_INVENTARIO));
    texto.textContent = `Pagina ${paginaActualInventario} de ${totalPaginasInventario}`;
    contenedor.style.display = totalPaginasInventario > 1 ? "flex" : "none";
    btnAnterior.disabled = paginaActualInventario === 1;
    btnSiguiente.disabled = paginaActualInventario === totalPaginasInventario;
}
