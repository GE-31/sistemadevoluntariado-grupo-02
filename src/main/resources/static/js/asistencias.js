// Variables globales
let modoEdicion = false;
let paginaActualAsis = 1;
const registrosPorPaginaAsis = 8;

// ========================
// MODAL
// ========================
function abrirModalRegistrar() {
    modoEdicion = false;
    document.getElementById('modalTitulo').textContent = 'Registrar Asistencia';
    document.getElementById('formAsistencia').reset();
    document.getElementById('asistenciaId').value = '';
    document.getElementById('modoEdicion').value = 'false';

    // Mostrar campos de voluntario, actividad y fecha (solo se ocultan en edición)
    document.getElementById('grupoVoluntario').style.display = '';
    document.getElementById('grupoActividad').style.display = '';
    document.getElementById('grupoFecha').style.display = '';

    // Fecha por defecto: hoy
    document.getElementById('fecha').value = new Date().toISOString().split('T')[0];

    document.getElementById('modalAsistencia').style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

function abrirModalEditar(id) {
    modoEdicion = true;
    document.getElementById('modalTitulo').textContent = 'Editar Asistencia';
    document.getElementById('modoEdicion').value = 'true';

    fetch(`asistencias?action=obtener&id=${id}`)
        .then(response => response.text())
        .then(text => {
            if (!text || text.trim() === '') {
                throw new Error('El servidor devolvió una respuesta vacía');
            }
            return JSON.parse(text);
        })
        .then(asistencia => {
            document.getElementById('asistenciaId').value = asistencia.idAsistencia;
            document.getElementById('id_voluntario').value = asistencia.idVoluntario;
            document.getElementById('id_actividad').value = asistencia.idActividad;
            document.getElementById('fecha').value = asistencia.fecha;
            document.getElementById('hora_entrada').value = asistencia.horaEntrada || '';
            document.getElementById('hora_salida').value = asistencia.horaSalida || '';
            document.getElementById('estado').value = asistencia.estado;
            document.getElementById('observaciones').value = asistencia.observaciones || '';

            // En edición ocultar voluntario, actividad y fecha (no se pueden cambiar)
            document.getElementById('grupoVoluntario').style.display = 'none';
            document.getElementById('grupoActividad').style.display = 'none';
            document.getElementById('grupoFecha').style.display = 'none';

            // Quitar required de los ocultos
            document.getElementById('id_voluntario').required = false;
            document.getElementById('id_actividad').required = false;
            document.getElementById('fecha').required = false;

            toggleHoras();

            document.getElementById('modalAsistencia').style.display = 'flex';
            document.body.style.overflow = 'hidden';
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error al cargar los datos de la asistencia.');
        });
}

function cerrarModal() {
    document.body.style.overflow = 'auto';
    document.getElementById('modalAsistencia').style.display = 'none';

    // Restaurar required
    document.getElementById('id_voluntario').required = true;
    document.getElementById('id_actividad').required = true;
    document.getElementById('fecha').required = true;
}

// ========================
// MOSTRAR FECHAS DE ACTIVIDAD
// ========================
function mostrarFechasActividad() {
    const select = document.getElementById('id_actividad');
    const info = document.getElementById('infoFechasActividad');
    const fechaInput = document.getElementById('fecha');
    const option = select.options[select.selectedIndex];

    if (!select.value) {
        if (info) info.style.display = 'none';
        fechaInput.removeAttribute('min');
        fechaInput.removeAttribute('max');
        return;
    }

    const fechaInicio = option.getAttribute('data-fecha-inicio') || '';
    const fechaFin = option.getAttribute('data-fecha-fin') || '';

    if (info) {
        document.getElementById('txtFechaInicio').textContent = fechaInicio ? formatearFechaCorta(fechaInicio) : '—';
        document.getElementById('txtFechaFin').textContent = fechaFin ? formatearFechaCorta(fechaFin) : '—';
        info.style.display = 'block';
    }

    // Restringir el input de fecha al rango de la actividad
    if (fechaInicio) fechaInput.setAttribute('min', fechaInicio);
    else fechaInput.removeAttribute('min');

    if (fechaFin) fechaInput.setAttribute('max', fechaFin);
    else fechaInput.removeAttribute('max');
}

function formatearFechaCorta(fechaStr) {
    if (!fechaStr || fechaStr === 'null') return '—';
    const partes = fechaStr.split('-');
    if (partes.length !== 3) return fechaStr;
    return partes[2] + '/' + partes[1] + '/' + partes[0]; // dd/mm/yyyy
}

// Mostrar/ocultar horas según estado
function toggleHoras() {
    const estado = document.getElementById('estado').value;
    const grupoEntrada = document.getElementById('grupoHoraEntrada');
    const grupoSalida = document.getElementById('grupoHoraSalida');

    if (estado === 'FALTA') {
        grupoEntrada.style.opacity = '0.5';
        grupoSalida.style.opacity = '0.5';
        document.getElementById('hora_entrada').value = '';
        document.getElementById('hora_salida').value = '';
    } else {
        grupoEntrada.style.opacity = '1';
        grupoSalida.style.opacity = '1';
    }
}

// ========================
// GUARDAR ASISTENCIA
// ========================
function guardarAsistencia(event) {
    event.preventDefault();

    const id = document.getElementById('asistenciaId').value;
    const esEdicion = document.getElementById('modoEdicion').value === 'true';

    const params = new URLSearchParams();

    if (esEdicion) {
        params.append('action', 'actualizar');
        params.append('id', id);
        params.append('hora_entrada', document.getElementById('hora_entrada').value);
        params.append('hora_salida', document.getElementById('hora_salida').value);
        params.append('estado', document.getElementById('estado').value);
        params.append('observaciones', document.getElementById('observaciones').value);
    } else {
        params.append('action', 'registrar');
        params.append('id_voluntario', document.getElementById('id_voluntario').value);
        params.append('id_actividad', document.getElementById('id_actividad').value);
        params.append('fecha', document.getElementById('fecha').value);
        params.append('hora_entrada', document.getElementById('hora_entrada').value);
        params.append('hora_salida', document.getElementById('hora_salida').value);
        params.append('estado', document.getElementById('estado').value);
        params.append('observaciones', document.getElementById('observaciones').value);
    }

    fetch('asistencias', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
    })
        .then(response => response.text())
        .then(text => {
            if (!text || text.trim() === '') {
                throw new Error('Respuesta vacía del servidor');
            }
            const data = JSON.parse(text);

            if (data.success) {
                cerrarModal();
                location.reload();
            } else {
                alert(data.message || 'Error al guardar la asistencia');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error al guardar la asistencia: ' + error.message);
        });
}

// ========================
// ELIMINAR ASISTENCIA
// ========================
function eliminarAsistencia(id) {
    if (!confirm('¿Estás seguro de eliminar esta asistencia?')) return;

    const params = new URLSearchParams();
    params.append('action', 'eliminar');
    params.append('id', id);

    fetch('asistencias', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: params.toString()
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                location.reload();
            } else {
                alert(data.message || 'Error al eliminar');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error al eliminar la asistencia');
        });
}

// ========================
// FILTRAR POR ACTIVIDAD
// ========================
function filtrarPorActividad() {
    aplicarFiltroActividadAsistencias();
}

function aplicarFiltroActividadAsistencias() {
    const idActividad = document.getElementById('filtroActividad')?.value || '';
    const filas = Array.from(document.querySelectorAll('#asistencias-tbody .asistencia-row'));

    filas.forEach((fila) => {
        const actividadFila = fila.dataset.actividadId || '';
        const coincideActividad = !idActividad || actividadFila === idActividad;
        fila.classList.toggle('filtro-oculto', !coincideActividad);
    });

    paginaActualAsis = 1;
    actualizarPaginacionAsis();
}

// ========================
// PAGINACIÓN
// ========================
function actualizarPaginacionAsis() {
    const tbody = document.getElementById('asistencias-tbody');
    if (!tbody) return;
    const totalFilasTabla = Array.from(document.querySelectorAll('#asistencias-tbody .asistencia-row')).length;

    const filas = Array.from(document.querySelectorAll('#asistencias-tbody .asistencia-row'))
        .filter(f => !f.classList.contains('filtro-oculto'));
    const total = filas.length;
    const totalPaginas = Math.ceil(total / registrosPorPaginaAsis) || 1;

    if (paginaActualAsis > totalPaginas) paginaActualAsis = totalPaginas;

    const inicio = (paginaActualAsis - 1) * registrosPorPaginaAsis;
    const fin = inicio + registrosPorPaginaAsis;

    // Primero ocultar todas las visibles, luego mostrar solo la página actual
    const todasFilas = document.querySelectorAll('#asistencias-tbody .asistencia-row');
    todasFilas.forEach(f => {
        f.classList.add('paginacion-oculta');
        f.style.display = 'none';
    });

    filas.forEach((fila, index) => {
        if (index >= inicio && index < fin) {
            fila.classList.remove('paginacion-oculta');
            fila.style.display = '';
        } else {
            fila.classList.add('paginacion-oculta');
            fila.style.display = 'none';
        }
    });

    let filaNoResultados = document.getElementById('sinResultadosAsistenciasRow');
    if (!filaNoResultados) {
        filaNoResultados = document.createElement('tr');
        filaNoResultados.id = 'sinResultadosAsistenciasRow';
        filaNoResultados.innerHTML = '<td colspan="9" style="text-align:center; padding:2rem; color:#999;">No hay asistencias que coincidan con la busqueda</td>';
        tbody.appendChild(filaNoResultados);
    }
    filaNoResultados.style.display = total === 0 ? '' : 'none';

    const sinRegistros = document.getElementById('sinAsistenciasRow');
    if (sinRegistros) sinRegistros.style.display = totalFilasTabla === 0 ? '' : 'none';
    if (totalFilasTabla === 0) filaNoResultados.style.display = 'none';

    // Info
    const info = document.getElementById('paginacionInfo');
    if (info) {
        info.textContent = total > 0
            ? `Mostrando ${Math.min(inicio + 1, total)}-${Math.min(fin, total)} de ${total}`
            : 'Mostrando 0-0 de 0';
    }

    // Botones
    const btnPrev = document.getElementById('btnPrevAsis');
    const btnNext = document.getElementById('btnNextAsis');
    if (btnPrev) btnPrev.disabled = paginaActualAsis <= 1;
    if (btnNext) btnNext.disabled = paginaActualAsis >= totalPaginas;

    const paginacionEl = document.getElementById('paginacionAsistencias');
    if (paginacionEl) paginacionEl.style.display = totalFilasTabla > 0 && total > 0 ? '' : 'none';

    // Páginas
    const pagesContainer = document.getElementById('paginacionPages');
    if (pagesContainer) {
        pagesContainer.innerHTML = '';
        for (let i = 1; i <= totalPaginas; i++) {
            const btn = document.createElement('button');
            btn.textContent = i;
            btn.style.cssText = 'padding:0.4rem 0.7rem; border:1px solid #e5e7eb; background:' +
                (i === paginaActualAsis ? '#667eea' : '#fff') + '; color:' +
                (i === paginaActualAsis ? '#fff' : '#4b5563') +
                '; border-radius:6px; font-size:0.82rem; cursor:pointer;';
            btn.onclick = () => { paginaActualAsis = i; actualizarPaginacionAsis(); };
            pagesContainer.appendChild(btn);
        }
    }
}

function cambiarPaginaAsis(dir) {
    paginaActualAsis += dir;
    actualizarPaginacionAsis();
}

// ========================
// CARGAR ASISTENCIAS VÍA AJAX
// ========================
function cargarAsistencias() {
    fetch('asistencias?action=listar')
        .then(response => response.json())
        .then(asistencias => {
            const tbody = document.getElementById('asistencias-tbody');
            tbody.innerHTML = '';

            if (asistencias && asistencias.length > 0) {
                asistencias.forEach(a => {
                    const fila = document.createElement('tr');
                    fila.className = 'asistencia-row';
                    fila.dataset.id = a.idAsistencia;
                    fila.dataset.actividadId = a.idActividad || '';
                    fila.innerHTML = `
                        <td><strong>${a.nombreVoluntario || ''}</strong></td>
                        <td><span class="badge-dni">${a.dniVoluntario || '-'}</span></td>
                        <td>${a.nombreActividad || ''}</td>
                        <td>${a.fecha || ''}</td>
                        <td>${a.horaEntrada || '-'}</td>
                        <td>${a.horaSalida || '-'}</td>
                        <td>${a.horasTotales || '0.00'}</td>
                        <td>
                            <span class="estado-badge ${(a.estado || '').toLowerCase()}">
                                ${a.estado || ''}
                            </span>
                        </td>
                        <td class="acciones-cell">
                            <button class="btn-icon edit" onclick="abrirModalEditar(${a.idAsistencia})" title="Editar">✎</button>
                            <button class="btn-icon delete" onclick="eliminarAsistencia(${a.idAsistencia})" title="Eliminar">🗑</button>
                        </td>
                    `;
                    tbody.appendChild(fila);
                });
            } else {
                tbody.innerHTML = '<tr id="sinAsistenciasRow"><td colspan="9" style="text-align:center; padding:2rem; color:#999;">No hay asistencias registradas</td></tr>';
            }

            paginaActualAsis = 1;
            aplicarFiltroActividadAsistencias();
        })
        .catch(error => {
            console.error('Error al cargar asistencias:', error);
        });
}

// Inicializar al cargar
document.addEventListener('DOMContentLoaded', function () {
    cargarAsistencias();
});
