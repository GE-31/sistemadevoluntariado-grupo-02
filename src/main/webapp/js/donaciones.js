const modal = document.getElementById("modalDonacion");
const form = document.getElementById("formDonacion");
const tipoDonacionSelect = document.getElementById("tipoDonacion");
const seccionEspecie = document.getElementById("seccionEspecie");
const labelCantidad = document.getElementById("labelCantidad");
const btnGuardarDonacion = document.getElementById("btnGuardarDonacion");
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

function abrirModal() {
    modal.style.display = "flex";
    form.reset();
    document.getElementById("tituloModal").innerText = "Registrar Donacion";
    cargarActividades();
    cargarItemsInventario();
    actualizarCamposInventario();
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

function editarDonacion() {
    alert("La edicion de donaciones no esta habilitada en este flujo.");
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

function eliminarDonacion() {
    alert("La eliminacion de donaciones no esta habilitada en este flujo.");
}

if (tipoDonacionSelect) {
    tipoDonacionSelect.addEventListener("change", actualizarCamposInventario);
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
});
