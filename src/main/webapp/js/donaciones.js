const modal = document.getElementById("modalDonacion");
const form = document.getElementById("formDonacion");
const tipoDonacionSelect = document.getElementById("tipoDonacion");
const seccionEspecie = document.getElementById("seccionEspecie");
const labelCantidad = document.getElementById("labelCantidad");
const btnGuardarDonacion = document.getElementById("btnGuardarDonacion");
const accionDonacionInput = document.getElementById("accionDonacion");
const idDonacionInput = document.getElementById("idDonacion");
const motivoEdicionInput = document.getElementById("motivoEdicion");
const idItemSelect = document.getElementById("idItem");
const crearNuevoItemCheck = document.getElementById("crearNuevoItem");
const nuevoItemFields = document.getElementById("nuevoItemFields");
const itemCategoriaSelect = document.getElementById("itemCategoria");
const itemCategoriaOtro = document.getElementById("itemCategoriaOtro");
const itemCategoriaValor = document.getElementById("itemCategoriaValor");
const itemUnidadSelect = document.getElementById("itemUnidadMedida");
const itemUnidadOtro = document.getElementById("itemUnidadMedidaOtro");
const itemUnidadValor = document.getElementById("itemUnidadMedidaValor");
const donacionAnonimaCheck = document.getElementById("donacionAnonima");
const donanteFields = document.getElementById("donanteFields");
const buscarDonacionesInput = document.getElementById("buscarDonaciones");
const PAGINA_TAMANO = 5;
let paginaActual = 1;
let totalPaginas = 1;
let donacionOriginal = null;
let filasBaseDonaciones = [];
let filasFiltradasDonaciones = [];

function abrirModal() {
    modal.style.display = "flex";
    form.reset();
    donacionOriginal = null;
    if (accionDonacionInput) accionDonacionInput.value = "registrar";
    if (idDonacionInput) idDonacionInput.value = "";
    if (motivoEdicionInput) motivoEdicionInput.value = "";
    document.getElementById("tituloModal").innerText = "Registrar Donacion";
    if (btnGuardarDonacion) btnGuardarDonacion.textContent = "Registrar donacion";
    desbloquearCamposEdicion();
    const cargas = Promise.all([cargarActividades(), cargarItemsInventario()]);
    actualizarCamposInventario();
    return cargas;
}

function cerrarModal() {
    modal.style.display = "none";
}

async function cargarActividades() {
    try {
        const resp = await fetch("actividades?action=listar");
        const actividades = await resp.json();
        const select = document.getElementById("actividad");
        select.innerHTML = '<option value="">Seleccione actividad</option>';
        actividades.forEach((act) => {
            const option = document.createElement("option");
            option.value = act.idActividad;
            option.textContent = act.nombre;
            select.appendChild(option);
        });
    } catch (error) {
        console.error("Error al cargar actividades:", error);
    }
}

async function cargarItemsInventario() {
    if (!idItemSelect) {
        return;
    }
    try {
        const resp = await fetch("inventario?accion=listar");
        const items = await resp.json();
        idItemSelect.innerHTML = '<option value="">Seleccione item existente</option>';
        items
            .filter((item) => String(item.estado || "").toUpperCase() === "ACTIVO")
            .forEach((item) => {
            const option = document.createElement("option");
            option.value = item.idItem;
            option.textContent = `${item.nombre} (${item.categoria} - ${item.unidadMedida})`;
            idItemSelect.appendChild(option);
        });
    } catch (error) {
        console.error("Error al cargar items de inventario:", error);
    }
}

function onTipoDonacionChange() {
    actualizarCamposInventario();
}

function actualizarCamposInventario() {
    const esObjeto = tipoDonacionSelect && tipoDonacionSelect.value === "2";
    if (seccionEspecie) {
        seccionEspecie.style.display = esObjeto ? "block" : "none";
    }
    if (labelCantidad) {
        labelCantidad.textContent = esObjeto ? "Cantidad a ingresar *" : "Monto (S/) *";
    }
    if (btnGuardarDonacion) {
        btnGuardarDonacion.textContent = esObjeto
            ? "Registrar y actualizar inventario"
            : "Registrar donacion";
    }
    actualizarModoItem();
    actualizarDonante();
}

function actualizarModoItem() {
    const esObjeto = tipoDonacionSelect && tipoDonacionSelect.value === "2";
    const crearNuevo = crearNuevoItemCheck && crearNuevoItemCheck.checked;

    if (!esObjeto) {
        if (idItemSelect) {
            idItemSelect.required = false;
            idItemSelect.disabled = false;
            idItemSelect.value = "";
        }
        if (crearNuevoItemCheck) {
            crearNuevoItemCheck.checked = false;
        }
        if (nuevoItemFields) {
            nuevoItemFields.style.display = "none";
        }
        setRequiredNuevoItem(false);
        limpiarNuevoItem();
        return;
    }

    if (idItemSelect) {
        idItemSelect.required = !crearNuevo;
        idItemSelect.disabled = crearNuevo;
        if (crearNuevo) {
            idItemSelect.value = "";
        }
    }
    if (nuevoItemFields) {
        nuevoItemFields.style.display = crearNuevo ? "grid" : "none";
    }
    setRequiredNuevoItem(crearNuevo);
    if (!crearNuevo) {
        limpiarNuevoItem();
    }
}

function setRequiredNuevoItem(required) {
    ["itemNombre", "itemCategoria", "itemUnidadMedida"].forEach((id) => {
        const input = document.getElementById(id);
        if (input) {
            input.required = required;
        }
    });
    if (itemCategoriaOtro) {
        itemCategoriaOtro.required = required && itemCategoriaSelect?.value === "__OTRO__";
    }
    if (itemUnidadOtro) {
        itemUnidadOtro.required = required && itemUnidadSelect?.value === "__OTRO__";
    }
}

function limpiarNuevoItem() {
    ["itemNombre", "itemCategoria", "itemUnidadMedida", "itemCategoriaOtro", "itemUnidadMedidaOtro"].forEach((id) => {
        const input = document.getElementById(id);
        if (input) {
            input.value = "";
        }
    });
    if (itemCategoriaValor) {
        itemCategoriaValor.value = "";
    }
    if (itemUnidadValor) {
        itemUnidadValor.value = "";
    }
    toggleOtroCategoria();
    toggleOtraUnidad();
    const min = document.getElementById("itemStockMinimo");
    if (min) {
        min.value = "0";
    }
}

function toggleOtroCategoria() {
    const mostrarOtro = itemCategoriaSelect && itemCategoriaSelect.value === "__OTRO__";
    if (itemCategoriaOtro) {
        itemCategoriaOtro.style.display = mostrarOtro ? "block" : "none";
        itemCategoriaOtro.required = (crearNuevoItemCheck?.checked === true) && mostrarOtro;
        if (!mostrarOtro) {
            itemCategoriaOtro.value = "";
        }
    }
}

function toggleOtraUnidad() {
    const mostrarOtro = itemUnidadSelect && itemUnidadSelect.value === "__OTRO__";
    if (itemUnidadOtro) {
        itemUnidadOtro.style.display = mostrarOtro ? "block" : "none";
        itemUnidadOtro.required = (crearNuevoItemCheck?.checked === true) && mostrarOtro;
        if (!mostrarOtro) {
            itemUnidadOtro.value = "";
        }
    }
}

function normalizarCamposNuevoItemAntesDeEnviar() {
    if (itemCategoriaValor) {
        const categoriaFinal = itemCategoriaSelect?.value === "__OTRO__"
            ? (itemCategoriaOtro?.value || "").trim().toUpperCase()
            : (itemCategoriaSelect?.value || "").trim().toUpperCase();
        itemCategoriaValor.value = categoriaFinal;
    }
    if (itemUnidadValor) {
        const unidadFinal = itemUnidadSelect?.value === "__OTRO__"
            ? (itemUnidadOtro?.value || "").trim().toLowerCase()
            : (itemUnidadSelect?.value || "").trim().toLowerCase();
        itemUnidadValor.value = unidadFinal;
    }
}

async function editarDonacion(idDonacion) {
    try {
        const resp = await fetch(`donaciones?accion=obtener&id=${idDonacion}`);
        const data = await resp.json();
        if (!data || data.ok === false) {
            alert(data?.message || "No se pudo cargar la donacion");
            return;
        }

        donacionOriginal = data;
        await abrirModal();
        document.getElementById("tituloModal").innerText = "Editar Donacion";
        if (accionDonacionInput) accionDonacionInput.value = "editar";
        if (idDonacionInput) idDonacionInput.value = data.idDonacion || "";

        document.getElementById("tipoDonacion").value = String(data.idTipoDonacion || "");
        document.getElementById("cantidad").value = data.cantidad ?? "";
        document.getElementById("descripcion").value = data.descripcion || "";
        document.getElementById("actividad").value = String(data.idActividad || "");
        if (donacionAnonimaCheck) {
            donacionAnonimaCheck.checked = data.donacionAnonima === true;
        }
        document.getElementById("tipoDonante").value = (data.tipoDonante || "PERSONA").toUpperCase();
        document.getElementById("nombreDonante").value = data.nombreDonante || "";
        document.getElementById("correoDonante").value = data.correoDonante || "";
        document.getElementById("telefonoDonante").value = data.telefonoDonante || "";

        if (data.idItem) {
            idItemSelect.value = String(data.idItem);
        }

        actualizarCamposInventario();
        bloquearCamposSegunTipo(data.idTipoDonacion);
        if (btnGuardarDonacion) btnGuardarDonacion.textContent = "Guardar cambios";
    } catch (error) {
        console.error("Error cargando donacion para editar:", error);
        alert("No se pudo abrir la donacion para editar");
    }
}

function actualizarDonante() {
    const anonima = donacionAnonimaCheck && donacionAnonimaCheck.checked;
    if (donanteFields) {
        donanteFields.style.display = anonima ? "none" : "grid";
    }
    const nombreDonante = document.getElementById("nombreDonante");
    const tipoDonante = document.getElementById("tipoDonante");
    if (nombreDonante) {
        nombreDonante.required = !anonima;
        if (anonima) nombreDonante.value = "";
    }
    if (tipoDonante) {
        tipoDonante.required = !anonima;
    }
    if (anonima) {
        ["correoDonante", "telefonoDonante"].forEach((id) => {
            const input = document.getElementById(id);
            if (input) input.value = "";
        });
    }
}

async function anularDonacion(idDonacion) {
    const motivo = prompt("Motivo de anulacion (opcional):", "Anulacion manual");
    if (motivo === null) {
        return;
    }

    if (!confirm("Se anulara la donacion y, si fue en especie, se revertira su stock. ¿Continuar?")) {
        return;
    }

    const params = new URLSearchParams();
    params.append("accion", "anular");
    params.append("idDonacion", idDonacion);
    params.append("motivo", motivo || "Anulacion manual");

    try {
        const resp = await fetch("donaciones", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: params.toString()
        });
        const result = await resp.json();
        if (result.ok) {
            window.location.reload();
        } else {
            alert(result.message || "No se pudo anular la donacion");
        }
    } catch (error) {
        console.error("Error anulando donacion:", error);
        alert("Error al anular la donacion");
    }
}

function bloquearCamposSegunTipo(idTipo) {
    const esDinero = Number(idTipo) === 1;
    if (!esDinero) {
        const cantidad = document.getElementById("cantidad");
        if (cantidad) {
            cantidad.readOnly = true;
            cantidad.title = "Para donaciones en especie, la cantidad no se edita desde este flujo.";
        }
        if (tipoDonacionSelect) {
            tipoDonacionSelect.dataset.locked = "1";
            tipoDonacionSelect.dataset.original = String(idTipo);
        }
    }
}

function desbloquearCamposEdicion() {
    const cantidad = document.getElementById("cantidad");
    if (cantidad) {
        cantidad.readOnly = false;
        cantidad.title = "";
    }
    if (tipoDonacionSelect) {
        tipoDonacionSelect.dataset.locked = "0";
        tipoDonacionSelect.dataset.original = "";
    }
}

function inicializarPaginacionDonaciones() {
    const tbody = document.getElementById("tbodyDonaciones");
    if (!tbody) {
        return;
    }
    filasBaseDonaciones = Array.from(tbody.querySelectorAll("tr")).filter((tr) => !tr.querySelector(".no-data"));
    filasFiltradasDonaciones = [...filasBaseDonaciones];
    paginaActual = 1;
    renderPaginacionDonaciones();
}

function renderPaginacionDonaciones() {
    const tbody = document.getElementById("tbodyDonaciones");
    const contenedor = document.getElementById("donacionesPaginacion");
    const btnAnterior = document.getElementById("btnPaginaAnterior");
    const btnSiguiente = document.getElementById("btnPaginaSiguiente");
    const texto = document.getElementById("textoPaginacionDonaciones");
    if (!tbody || !contenedor || !btnAnterior || !btnSiguiente || !texto) return;

    const filaNoResultadosExistente = document.getElementById("filaNoResultadosDonaciones");
    if (filaNoResultadosExistente) {
        filaNoResultadosExistente.remove();
    }

    if (filasFiltradasDonaciones.length === 0) {
        filasBaseDonaciones.forEach((fila) => {
            fila.style.display = "none";
        });
        contenedor.style.display = "none";
        tbody.insertAdjacentHTML("beforeend", '<tr id="filaNoResultadosDonaciones"><td colspan="8" class="no-data">No hay donaciones que coincidan con la busqueda</td></tr>');
        return;
    }

    totalPaginas = Math.max(1, Math.ceil(filasFiltradasDonaciones.length / PAGINA_TAMANO));
    if (paginaActual > totalPaginas) {
        paginaActual = totalPaginas;
    }

    const inicio = (paginaActual - 1) * PAGINA_TAMANO;
    const fin = inicio + PAGINA_TAMANO;
    filasBaseDonaciones.forEach((fila) => {
        fila.style.display = "none";
    });
    filasFiltradasDonaciones.forEach((fila, idx) => {
        fila.style.display = idx >= inicio && idx < fin ? "" : "none";
    });

    texto.textContent = `Pagina ${paginaActual} de ${totalPaginas}`;
    contenedor.style.display = totalPaginas > 1 ? "flex" : "none";
    btnAnterior.disabled = paginaActual === 1;
    btnSiguiente.disabled = paginaActual === totalPaginas;
}

function aplicarFiltroDonaciones() {
    const q = (buscarDonacionesInput?.value || "").trim().toLowerCase();
    filasFiltradasDonaciones = filasBaseDonaciones.filter((fila) => {
        if (!q) return true;
        return (fila.textContent || "").toLowerCase().includes(q);
    });
    paginaActual = 1;
    renderPaginacionDonaciones();
}

function configurarEventosPaginacionDonaciones() {
    const btnAnterior = document.getElementById("btnPaginaAnterior");
    const btnSiguiente = document.getElementById("btnPaginaSiguiente");
    if (!btnAnterior || !btnSiguiente) return;

    btnAnterior.addEventListener("click", () => {
        if (paginaActual > 1) {
            paginaActual -= 1;
            renderPaginacionDonaciones();
        }
    });
    btnSiguiente.addEventListener("click", () => {
        if (paginaActual < totalPaginas) {
            paginaActual += 1;
            renderPaginacionDonaciones();
        }
    });
}

if (tipoDonacionSelect) {
    tipoDonacionSelect.addEventListener("change", () => {
        if (tipoDonacionSelect.dataset.locked === "1") {
            tipoDonacionSelect.value = tipoDonacionSelect.dataset.original || "";
            alert("No se puede cambiar el tipo de donacion en modo edicion.");
            return;
        }
        actualizarCamposInventario();
    });
}
if (crearNuevoItemCheck) {
    crearNuevoItemCheck.addEventListener("change", actualizarModoItem);
}
if (itemCategoriaSelect) {
    itemCategoriaSelect.addEventListener("change", toggleOtroCategoria);
}
if (itemUnidadSelect) {
    itemUnidadSelect.addEventListener("change", toggleOtraUnidad);
}
if (form) {
    form.addEventListener("submit", () => {
        normalizarCamposNuevoItemAntesDeEnviar();
        if (accionDonacionInput && accionDonacionInput.value === "editar" && motivoEdicionInput) {
            const motivo = prompt("Motivo de edicion (opcional):", "Actualizacion de donacion");
            if (motivo !== null) {
                motivoEdicionInput.value = (motivo || "Actualizacion de donacion").trim();
            } else {
                motivoEdicionInput.value = "Actualizacion de donacion";
            }
        }
    });
}
if (donacionAnonimaCheck) {
    donacionAnonimaCheck.addEventListener("change", actualizarDonante);
}

document.addEventListener("DOMContentLoaded", () => {
    actualizarCamposInventario();
    toggleOtroCategoria();
    toggleOtraUnidad();
    actualizarDonante();
    configurarEventosPaginacionDonaciones();
    inicializarPaginacionDonaciones();
    if (buscarDonacionesInput) {
        buscarDonacionesInput.addEventListener("input", aplicarFiltroDonaciones);
    }
});
