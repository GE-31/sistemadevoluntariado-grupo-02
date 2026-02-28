// ===============================
// UTILIDADES
// ===============================

// Detecta el context path real del proyecto
function getContextPath() {
    const path = window.location.pathname;
    const index = path.indexOf("/", 1);
    return index > 0 ? path.substring(0, index) : "";
}

// Convierte fecha a formato legible
function formatDate(dateString) {
    if (!dateString) return "";
    const d = new Date(dateString);
    return d.toLocaleDateString("es-PE", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
    });
}

// Convierte números a S/ 0.00
function formatMoney(value) {
    if (!value || isNaN(value)) return "";
    return "S/ " + Number(value).toFixed(2);
}

// Badge para valores
function badge(text, type) {
    return `<span class="badge ${type}">${text}</span>`;
}

// ===============================
// DESCARGA DE EXCEL
// ===============================

async function downloadReport(paramsObj = {}) {
    const ctx = getContextPath();
    const params = new URLSearchParams({ accion: "donaciones" });

    for (const k in paramsObj) {
        if (paramsObj[k]) params.append(k, paramsObj[k]);
    }

    try {
        const resp = await fetch(`${ctx}/reportes?${params.toString()}`);
        if (!resp.ok) throw new Error("Error " + resp.status);

        const blob = await resp.blob();
        const disp = resp.headers.get("Content-Disposition") || "";
        const fileMatch = /filename="?([^"]+)"?/.exec(disp);
        const filename = fileMatch ? fileMatch[1] : "reportes.xlsx";

        const link = document.createElement("a");
        link.href = URL.createObjectURL(blob);
        link.download = filename;
        link.click();
    } catch (err) {
        console.error("Error descargando reporte:", err);
        alert("No se pudo descargar el archivo.");
    }
}

// ===============================
// EVENTOS
// ===============================

document.addEventListener("DOMContentLoaded", () => {

    const btnExport = document.getElementById("btnExportDonacionesMain");
    const btnBuscar = document.getElementById("btnBuscarReportes");

    const fInicio = document.getElementById("fInicio");
    const fFin = document.getElementById("fFin");
    const fTipo = document.getElementById("fTipoDonacion");
    const fDonante = document.getElementById("fTipoDonante");
    const fIncluirAnuladas = document.getElementById("fIncluirAnuladas");

    // Habilitar botones solo si hay fechas
    function validateRange() {
        const enabled = fInicio.value && fFin.value;
        if (btnExport) btnExport.disabled = !enabled;
        if (btnBuscar) btnBuscar.disabled = !enabled;
    }
    fInicio?.addEventListener("input", validateRange);
    fFin?.addEventListener("input", validateRange);
    validateRange();

    // Exportar
    btnExport?.addEventListener("click", () => {
        downloadReport({
            fechaInicio: fInicio.value,
            fechaFin: fFin.value,
            tipoDonacion: fTipo.value,
            tipoDonante: fDonante.value,
            incluirAnuladas: fIncluirAnuladas && fIncluirAnuladas.checked ? '1' : ''
        });
    });

    // Buscar (mostrar tabla)
    btnBuscar?.addEventListener("click", async () => {
        const ctx = getContextPath();

        const params = new URLSearchParams({
            accion: "listar",
            fechaInicio: fInicio.value,
            fechaFin: fFin.value
        });

        if (fTipo.value) params.append("tipoDonacion", fTipo.value);
        if (fDonante.value) params.append("tipoDonante", fDonante.value);
        if (fIncluirAnuladas && fIncluirAnuladas.checked) params.append('incluirAnuladas','1');

        const resp = await fetch(`${ctx}/reportes?${params.toString()}`);
        const data = await resp.json();

        renderReportesResults(data);
    });
});

// ===============================
// RENDER DE TABLA
// ===============================

function renderReportesResults(list) {
    const container = document.getElementById("reportesResults");
    if (!container) return;

    if (!list || list.length === 0) {
        container.innerHTML = `
            <div class="empty-list">
                <div class="empty-icon">📭</div>
                <div class="empty-text">No hay resultados para este período</div>
            </div>`;
        return;
    }

    let html = `
    <div class="reportes-table">
        <table>
            <thead>
                <tr>
                    <th>Fecha</th>
                    <th>Tipo</th>
                    <th>Monto</th>
                    <th>Donante</th>
                    <th>Nombre / Razón Social</th>
                    <th>Correo</th>
                    <th>Teléfono</th>
                    <th>Actividad</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                </tr>
            </thead>
            <tbody>
    `;

    for (const r of list) {
        const tipoBadge = (r.idTipoDonacion === 1 || String(r.tipoDonacion).toUpperCase().includes('DINERO'))
            ? badge("Dinero", "success")
            : badge("En especie", "warning");

        // Normalizar estado para UI: null/empty -> PENDIENTE, ACTIVO (legacy) -> CONFIRMADO
        let estadoRaw = (r.estado || '').toUpperCase();
        let estadoNorm = estadoRaw;
        if (!estadoNorm) estadoNorm = 'PENDIENTE';
        if (estadoNorm === 'ACTIVO') estadoNorm = 'CONFIRMADO';

        let estadoClass = 'warning';
        if (estadoNorm === 'CONFIRMADO') estadoClass = 'success';
        if (estadoNorm === 'ANULADO' || estadoNorm === 'RECHAZADO') estadoClass = 'error';

        const estadoBadge = badge(estadoNorm, estadoClass);
        const ident = r.dniDonante || r.rucDonante || "";

        // acciones: mostrar Aprobar si NO es CONFIRMADO y NO está ANULADO
        const acciones = (estadoNorm !== 'CONFIRMADO' && estadoNorm !== 'ANULADO')
            ? `<button class="btn-approve" data-id="${r.idDonacion}">Aprobar</button>`
            : `-`;

        html += `
            <tr>
                <td>${formatDate(r.registradoEn)}</td>
                <td>${tipoBadge}</td>
                <td>${formatMoney(r.cantidad)}</td>
                <td>${r.tipoDonante ?? ""}</td>
                <td>${r.nombreDonante ?? ""}</td>
                <td>${r.correoDonante ?? ""}</td>
                <td>${r.telefonoDonante ?? ""}</td>
                <td>${r.actividad ?? ""}</td>
                <td>${estadoBadge}</td>
                <td>${acciones}</td>
        `;
    }

    html += `</tbody></table></div>`;
    container.innerHTML = html;

    // Delegación de eventos para botones Aprobar
    container.querySelectorAll('.btn-approve').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const id = btn.getAttribute('data-id');
            if (!confirm('¿Aprobar esta donación?')) return;
            try {
                const params = new URLSearchParams();
                params.append('accion', 'cambiar_estado');
                params.append('idDonacion', id);
                params.append('estado', 'ACTIVO');

                const resp = await fetch(getContextPath() + '/donaciones', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: params.toString()
                });
                const text = await resp.text();
                // El servlet devuelve JSON
                let obj = {};
                try { obj = JSON.parse(text); } catch (err) { obj = { ok: false, message: text }; }
                if (obj.ok) {
                    alert('Donación aprobada');
                    // refrescar la búsqueda actual
                    document.getElementById('btnBuscarReportes')?.click();
                } else {
                    alert('Error: ' + (obj.message || 'No se pudo aprobar')); 
                }
            } catch (err) {
                console.error(err);
                alert('Error al aprobar la donación');
            }
        });
    });
}

