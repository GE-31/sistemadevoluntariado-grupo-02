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
const donacionAnonimaCheck = document.getElementById("donacionAnonima");
const donanteFields = document.getElementById("donanteFields");
const dniDonanteInput = document.getElementById("dniDonante");
const buscarDonacionesInput = document.getElementById("buscarDonaciones");
const PAGINA_TAMANO = 5;

// --- Lookup (DNI / RUC) using your provided APIs ---
// NOTE: token is exposed client-side because you provided the URL/token. If you prefer the token secret
// I can add a server-side proxy endpoint that calls the external API and avoids CORS / token exposure.
const RUC_API_TEMPLATE = 'https://dniruc.apisperu.com/api/v1/ruc/{ident}?token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJlbWFpbCI6ImtmZW50eWdlbjAyNDVAZ21haWwuY29tIn0.JaGeW7g-XS7gcGqBFj7_mnNTgXwFg-THOxxgkLsfJaU';
const DNI_API_TEMPLATE = 'https://dniruc.apisperu.com/api/v1/dni/{ident}?token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJlbWFpbCI6ImtmZW50eWdlbjAyNDVAZ21haWwuY29tIn0.JaGeW7g-XS7gcGqBFj7_mnNTgXwFg-THOxxgkLsfJaU';

function debounce(fn, delay = 400) {
    let timer = null;
    return function(...args) {
        clearTimeout(timer);
        timer = setTimeout(() => fn.apply(this, args), delay);
    };
}

async function fetchDonanteByIdent() {
    if (!dniDonanteInput) return;
    const tipo = (tipoDonanteSelect?.value || 'PERSONA').toUpperCase();
    const ident = (dniDonanteInput.value || '').trim();
    const statusEl = document.getElementById('identLookupStatus');
    if (!ident) {
        if (statusEl) statusEl.textContent = '';
        return;
    }
    if (tipo === 'PERSONA' && ident.length !== 8) {
        if (statusEl) statusEl.textContent = '';
        return;
    }
    if ((tipo === 'EMPRESA' || tipo === 'GRUPO') && ident.length !== 11) {
        if (statusEl) statusEl.textContent = '';
        return;
    }

    if (statusEl) statusEl.textContent = 'Buscando información...';
    try {
        const url = tipo === 'PERSONA'
            ? DNI_API_TEMPLATE.replace('{ident}', encodeURIComponent(ident))
            : RUC_API_TEMPLATE.replace('{ident}', encodeURIComponent(ident));
        const resp = await fetch(url);
        if (!resp.ok) throw new Error('No encontrado');
        const data = await resp.json();

        let nombre = '';
        if (tipo === 'PERSONA') {
            nombre = data.nombre_completo || data.nombre_completo_inei || data.nombre || (data.nombres ? ([data.nombres, data.apellidoPaterno, data.apellidoMaterno].filter(Boolean).join(' ')) : '');
        } else {
            nombre = data.razon_social || data.nombre_o_razon_social || data.razonSocial || data.nombre || '';
        }

        if (nombre) {
            const nombreInput = document.getElementById('nombreDonante');
            if (nombreInput) nombreInput.value = nombre;
            if (tipo === 'PERSONA') {
                if (tipoDonanteSelect) tipoDonanteSelect.value = 'PERSONA';
            } else {
                if (tipoDonanteSelect) tipoDonanteSelect.value = 'EMPRESA';
            }
            actualizarDonante();
            if (statusEl) statusEl.textContent = 'Autocompletado';
        } else {
            if (statusEl) statusEl.textContent = 'No se encontró nombre en la respuesta';
        }
    } catch (err) {
        console.error('Lookup identificador:', err);
        if (statusEl) statusEl.textContent = 'Error al consultar API';
    }
    setTimeout(() => {
        const statusEl2 = document.getElementById('identLookupStatus');
        if (statusEl2) statusEl2.textContent = '';
    }, 3500);
}

const debouncedFetchIdent = debounce(fetchDonanteByIdent, 450);

if (dniDonanteInput) {
    dniDonanteInput.addEventListener('input', function () {
        const tipo = (tipoDonanteSelect?.value || 'PERSONA').toUpperCase();
        const v = (this.value || '').trim();
        if ((tipo === 'PERSONA' && v.length === 8) || ((tipo === 'EMPRESA' || tipo === 'GRUPO') && v.length === 11)) {
            debouncedFetchIdent();
        } else {
            const st = document.getElementById('identLookupStatus');
            if (st) st.textContent = '';
        }
    });
    dniDonanteInput.addEventListener('blur', function () {
        fetchDonanteByIdent();
    });
}

// Escuchar cambios para actualizar campos del donante
const tipoDonanteSelect = document.getElementById("tipoDonante");
if (tipoDonanteSelect) tipoDonanteSelect.addEventListener("change", actualizarDonante);
if (donacionAnonimaCheck) donacionAnonimaCheck.addEventListener("change", actualizarDonante);

// Validación antes de enviar (regla: Empresa/Grupo -> RUC + (correo|telefono); Persona -> nombre + (correo|telefono))
if (form) {
    form.addEventListener("submit", function (e) {
        const anonima = donacionAnonimaCheck && donacionAnonimaCheck.checked;
        if (anonima) return; // si es anónima no validamos campos del donante aquí

        const tipoVal = (tipoDonanteSelect ? (tipoDonanteSelect.value || "PERSONA") : "PERSONA").toUpperCase();
        const identVal = (document.getElementById("dniDonante")?.value || "").trim();
        const nombre = (document.getElementById("nombreDonante")?.value || "").trim();
        const correo = (document.getElementById("correoDonante")?.value || "").trim();
        const telefono = (document.getElementById("telefonoDonante")?.value || "").trim();

        if (tipoVal === "PERSONA") {
            if (!identVal) {
                alert("Para persona: ingresa el DNI del donante.");
                e.preventDefault();
                return false;
            }
            if (!nombre) {
                alert("Para persona: ingresa el nombre del donante.");
                e.preventDefault();
                return false;
            }
            if (!correo && !telefono) {
                alert("Para persona: ingresa al menos correo o teléfono del donante.");
                e.preventDefault();
                return false;
            }
        } else {
            // Empresa / Grupo -> identVal contiene RUC en este modo
            if (!identVal) {
                alert("Para empresa/grupo es obligatorio el RUC.");
                e.preventDefault();
                return false;
            }
            if (!nombre) {
                alert("Para empresa/grupo: ingresa la razón social del donante.");
                e.preventDefault();
                return false;
            }
            if (!correo && !telefono) {
                alert("Para empresa/grupo: ingresa al menos correo o teléfono del donante.");
                e.preventDefault();
                return false;
            }
        }

        // Normalizar campos (mantener lógica existente)
        normalizarCamposNuevoItemAntesDeEnviar();
        return true;
    });
}
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
        const resp = await fetch("donaciones?accion=actividades");
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
        // Filtrar solo items ACTIVO
        const activos = (items || []).filter(i => i.estado === 'ACTIVO');
        idItemSelect.innerHTML = '<option value="">Seleccione item del inventario</option>';

        // Agrupar por categoría
        const grupos = {};
        activos.forEach(item => {
            const cat = item.categoria || 'Sin categoría';
            if (!grupos[cat]) grupos[cat] = [];
            grupos[cat].push(item);
        });

        // Crear optgroups
        Object.keys(grupos).sort().forEach(cat => {
            const optgroup = document.createElement("optgroup");
            optgroup.label = cat;
            grupos[cat].forEach(item => {
                const option = document.createElement("option");
                option.value = item.idItem;
                option.textContent = item.nombre + " (" + item.unidadMedida + " - stock: " + item.stockActual + ")";
                optgroup.appendChild(option);
            });
            idItemSelect.appendChild(optgroup);
        });

        // Si no hay items activos, deshabilitar la opción "En especie" y mostrar aviso
        const tipoEspecieOpt = document.querySelector('#tipoDonacion option[value="2"]');
        const noItemsNotice = document.getElementById('noItemsNotice');
        if (activos.length === 0) {
            if (tipoEspecieOpt) {
                tipoEspecieOpt.disabled = true;
                tipoEspecieOpt.title = 'No hay items activos en el inventario. Crea un item desde Inventario.';
            }
            if (noItemsNotice) noItemsNotice.style.display = 'block';
        } else {
            if (tipoEspecieOpt) {
                tipoEspecieOpt.disabled = false;
                tipoEspecieOpt.title = '';
            }
            if (noItemsNotice) noItemsNotice.style.display = 'none';
        }
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
    if (idItemSelect) {
        idItemSelect.required = esObjeto;
        idItemSelect.disabled = false;
        if (!esObjeto) idItemSelect.value = "";
    }
}

function normalizarCamposNuevoItemAntesDeEnviar() {
    // Ya no se crean items desde donaciones — se usan los existentes del inventario
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
        document.getElementById("cantidad").value = (data.cantidadItem ?? data.cantidad ?? "");
        document.getElementById("descripcion").value = data.descripcion || "";
        document.getElementById("actividad").value = String(data.idActividad || "");
        if (donacionAnonimaCheck) {
            donacionAnonimaCheck.checked = data.donacionAnonima === true;
        }
        document.getElementById("tipoDonante").value = (data.tipoDonante || "PERSONA").toUpperCase();
        const identEl = document.getElementById("dniDonante");
        const tipoDon = (data.tipoDonante || "PERSONA").toUpperCase();
        if (identEl) {
            identEl.value = (tipoDon === 'PERSONA') ? (data.dniDonante || '') : (data.rucDonante || '');
            // name will be ajusted by actualizarDonante()
        }
        document.getElementById("nombreDonante").value = data.nombreDonante || "";
        document.getElementById("correoDonante").value = data.correoDonante || "";
        document.getElementById("telefonoDonante").value = data.telefonoDonante || "";

        if (data.idItem) {
            idItemSelect.value = String(data.idItem);
        }

        actualizarCamposInventario();
        bloquearCamposSegunTipo(data.idTipoDonacion, data.estado);
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
    const correo = document.getElementById("correoDonante");
    const telefono = document.getElementById("telefonoDonante");
    const identEl = document.getElementById("dniDonante");
    const labelIdent = document.getElementById("labelIdent");

    // Nombre / Razón social: obligatorio para Persona y Empresa/Grupo cuando no es anónima
    if (nombreDonante) {
        const tipoActual = (tipoDonante ? (tipoDonante.value || "PERSONA") : "PERSONA").toUpperCase();
        const requiereNombre = !anonima && (tipoActual === "PERSONA" || tipoActual === "EMPRESA" || tipoActual === "GRUPO");
        nombreDonante.required = requiereNombre;
        if (anonima) {
            nombreDonante.value = "";
        }
        // actualizar label y placeholder según tipo
        const labelNombre = document.getElementById("labelNombreDonante");
        if (labelNombre) {
            if (tipoActual === "PERSONA") {
                labelNombre.textContent = "Nombre del donante *";
                nombreDonante.placeholder = "Nombres y apellidos";
            } else {
                labelNombre.textContent = "Razón social del donante *";
                nombreDonante.placeholder = "Razón social";
            }
        }
    }

    if (tipoDonante) {
        tipoDonante.required = !anonima;
    }

    // Campo identificador (DNI <-> RUC) — usamos el mismo input `dniDonante` y cambiamos label/name/placeholder
    const tipoVal = (tipoDonante ? (tipoDonante.value || "PERSONA") : "PERSONA").toUpperCase();
    if (identEl && labelIdent) {
        if (anonima) {
            // ocultar
            identEl.style.display = "none";
            identEl.required = false;
            identEl.value = "";
            labelIdent.style.display = "none";
            identEl.name = "";
        } else if (tipoVal === "PERSONA") {
            labelIdent.textContent = "DNI";
            labelIdent.style.display = "block";
            identEl.style.display = "block";
            identEl.placeholder = "Requerido para Persona";
            identEl.required = true;
            identEl.name = "dniDonante";
        } else {
            labelIdent.textContent = "RUC";
            labelIdent.style.display = "block";
            identEl.style.display = "block";
            identEl.placeholder = "Requerido para Empresa/Grupo";
            identEl.required = true;
            identEl.name = "rucDonante";
        }

        // Si ya hay un identificador pero el nombre está vacío, intentar autocompletar (DNI->persona, RUC->empresa)
        const nombreInput = document.getElementById("nombreDonante");
        if (identEl && nombreInput && !nombreInput.value) {
            const valIdent = (identEl.value || "").trim();
            if ((tipoVal === "PERSONA" && valIdent.length === 8) || ((tipoVal === "EMPRESA" || tipoVal === "GRUPO") && valIdent.length === 11)) {
                debouncedFetchIdent();
            }
        }
    }

    // Contactos: no forzamos ambos, validación en submit exige al menos uno (correo o teléfono)
    if (correo) {
        if (anonima) correo.value = "";
    }
    if (telefono) {
        if (anonima) telefono.value = "";
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

// Cambiar estado (ej. Aprobar -> ACTIVO)
async function cambiarEstadoDonacion(idDonacion, nuevoEstado) {
    if (!confirm(`¿Cambiar estado de la donación #${idDonacion} a ${nuevoEstado}?`)) return;
    try {
        const params = new URLSearchParams();
        params.append('accion', 'cambiar_estado');
        params.append('idDonacion', idDonacion);
        params.append('estado', nuevoEstado);

        const resp = await fetch('donaciones', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: params.toString()
        });
        const text = await resp.text();
        let obj = {};
        try { obj = JSON.parse(text); } catch(e) { obj = { ok: false, message: text }; }
        if (obj.ok) {
            alert(obj.message || 'Estado actualizado');
            location.reload();
        } else {
            alert('Error: ' + (obj.message || 'No se pudo actualizar el estado'));
        }
    } catch (err) {
        console.error(err);
        alert('Error al cambiar el estado de la donación');
    }
}

function bloquearCamposSegunTipo(idTipo, estado) {
    const esDinero = Number(idTipo) === 1;
    const estadoNorm = String(estado || "").toUpperCase();
    const bloquearCantidadEspecie = !esDinero && estadoNorm !== "PENDIENTE";
    if (bloquearCantidadEspecie) {
        const cantidad = document.getElementById("cantidad");
        if (cantidad) {
            cantidad.readOnly = true;
            cantidad.title = "Para donaciones en especie confirmadas/anuladas, la cantidad no se edita desde este flujo.";
        }
    } else {
        const cantidad = document.getElementById("cantidad");
        if (cantidad) {
            cantidad.readOnly = false;
            cantidad.title = "";
        }
    }
    if (!esDinero) {
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
if (form) {
    form.addEventListener("submit", (e) => {
        // Normalizar (si hubiera campos de nuevo item - mantenemos por compatibilidad)
        normalizarCamposNuevoItemAntesDeEnviar();

        // Validación cliente: donación en especie requiere seleccionar una categoria
        if (tipoDonacionSelect && tipoDonacionSelect.value === "2") {
            if (!idItemSelect || !idItemSelect.value) {
                e.preventDefault();
                alert('Para donaciones en especie debes seleccionar un item del inventario.');
                return;
            }
        }

        // En edición pedir motivo (comportamiento existente)
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
    actualizarDonante();
    configurarEventosPaginacionDonaciones();
    inicializarPaginacionDonaciones();
    if (buscarDonacionesInput) {
        buscarDonacionesInput.addEventListener("input", aplicarFiltroDonaciones);
    }
    // Configurar evento "Otro" en unidad del modal rápido
    const qiUnidad = document.getElementById("qi_unidad");
    if (qiUnidad) {
        qiUnidad.addEventListener("change", () => {
            const otro = document.getElementById("qi_unidadOtro");
            if (otro) {
                const mostrar = qiUnidad.value === "__OTRO__";
                otro.style.display = mostrar ? "block" : "none";
                otro.required = mostrar;
                if (!mostrar) otro.value = "";
            }
        });
    }
});

/* ===================================================================
   CREACIÓN RÁPIDA DE ITEM DESDE DONACIONES
=================================================================== */

async function cargarCategoriasParaQuickItem() {
    const sel = document.getElementById("qi_categoria");
    if (!sel) return;
    try {
        const resp = await fetch("inventario?accion=listar_categorias");
        const cats = await resp.json();
        sel.innerHTML = '<option value="">Seleccione</option>';
        (cats || []).forEach(c => {
            const opt = document.createElement("option");
            opt.value = c.nombre;
            opt.textContent = c.nombre;
            sel.appendChild(opt);
        });
    } catch (e) {
        console.error("Error cargando categorías para quick item:", e);
    }
}

function abrirModalNuevoItemRapido() {
    const modal = document.getElementById("modalNuevoItemRapido");
    if (!modal) return;
    const form = document.getElementById("formNuevoItemRapido");
    if (form) form.reset();
    const otro = document.getElementById("qi_unidadOtro");
    if (otro) { otro.style.display = "none"; otro.required = false; }
    cargarCategoriasParaQuickItem();
    modal.style.display = "flex";
}

function cerrarModalNuevoItemRapido() {
    const modal = document.getElementById("modalNuevoItemRapido");
    if (modal) modal.style.display = "none";
}

async function guardarItemRapido(event) {
    event.preventDefault();
    const btn = document.getElementById("btnGuardarItemRapido");
    if (btn) { btn.disabled = true; btn.textContent = "Creando..."; }

    const unidadSel = document.getElementById("qi_unidad").value;
    let unidad = unidadSel;
    if (unidadSel === "__OTRO__") {
        unidad = (document.getElementById("qi_unidadOtro").value || "").trim();
        if (!unidad) { alert("Especifique la unidad de medida"); if (btn) { btn.disabled = false; btn.innerHTML = '<i class="fa-solid fa-check"></i> Crear y seleccionar'; } return; }
    }

    const params = new URLSearchParams();
    params.append("accion", "registrar");
    params.append("nombre", document.getElementById("qi_nombre").value.trim());
    params.append("categoria", document.getElementById("qi_categoria").value);
    params.append("unidadMedida", unidad);
    params.append("stockMinimo", document.getElementById("qi_stockMinimo").value);
    params.append("observacion", (document.getElementById("qi_observacion").value || "").trim());

    try {
        const resp = await fetch("inventario", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: params.toString()
        });
        const result = await resp.json();
        if (result.ok) {
            cerrarModalNuevoItemRapido();
            // Recargar items y seleccionar el nuevo
            await cargarItemsInventario();
            if (result.idItem && idItemSelect) {
                idItemSelect.value = String(result.idItem);
            }
            // Habilitar la opcion especie si estaba deshabilitada
            const tipoEspecieOpt = document.querySelector('#tipoDonacion option[value="2"]');
            if (tipoEspecieOpt) tipoEspecieOpt.disabled = false;
            const noItemsNotice = document.getElementById('noItemsNotice');
            if (noItemsNotice) noItemsNotice.style.display = 'none';
        } else {
            alert(result.message || "No se pudo crear el item");
        }
    } catch (e) {
        console.error("Error creando item rápido:", e);
        alert("Error al crear el item");
    } finally {
        if (btn) { btn.disabled = false; btn.innerHTML = '<i class="fa-solid fa-check"></i> Crear y seleccionar'; }
    }
}
