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
    const idActividad = document.getElementById('filtroActividad').value;
    const filas = document.querySelectorAll('#asistencias-tbody .asistencia-row');
    const sinRegistros = document.getElementById('sinAsistenciasRow');

    if (!idActividad) {
        // Mostrar todas
        filas.forEach(f => f.style.display = '');
        if (sinRegistros) sinRegistros.style.display = filas.length === 0 ? '' : 'none';
        paginaActualAsis = 1;
        actualizarPaginacionAsis();
        return;
    }

    // Filtrar por actividad usando fetch
    fetch(`asistencias?action=porActividad&id_actividad=${idActividad}`)
        .then(response => response.json())
        .then(data => {
            const ids = data.map(a => String(a.idAsistencia));
            let visibles = 0;

            filas.forEach(fila => {
                if (ids.includes(fila.dataset.id)) {
                    fila.style.display = '';
                    visibles++;
                } else {
                    fila.style.display = 'none';
                }
            });

            if (sinRegistros) {
                sinRegistros.style.display = visibles === 0 ? '' : 'none';
            }

            paginaActualAsis = 1;
            actualizarPaginacionAsis();
        })
        .catch(error => console.error('Error al filtrar:', error));
}

// ========================
// PAGINACIÓN
// ========================
function actualizarPaginacionAsis() {
    const filas = Array.from(document.querySelectorAll('#asistencias-tbody .asistencia-row'))
        .filter(f => f.style.display !== 'none');
    const total = filas.length;
    const totalPaginas = Math.ceil(total / registrosPorPaginaAsis) || 1;

    if (paginaActualAsis > totalPaginas) paginaActualAsis = totalPaginas;

    const inicio = (paginaActualAsis - 1) * registrosPorPaginaAsis;
    const fin = inicio + registrosPorPaginaAsis;

    // Primero ocultar todas las visibles, luego mostrar solo la página actual
    const todasFilas = document.querySelectorAll('#asistencias-tbody .asistencia-row');
    todasFilas.forEach(f => {
        if (f.style.display !== 'none') {
            f.classList.add('paginacion-oculta');
        }
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

    // Info
    const info = document.getElementById('paginacionInfo');
    if (info) {
        info.textContent = `Mostrando ${Math.min(inicio + 1, total)}-${Math.min(fin, total)} de ${total}`;
    }

    // Botones
    const btnPrev = document.getElementById('btnPrevAsis');
    const btnNext = document.getElementById('btnNextAsis');
    if (btnPrev) btnPrev.disabled = paginaActualAsis <= 1;
    if (btnNext) btnNext.disabled = paginaActualAsis >= totalPaginas;

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

// Inicializar paginación al cargar
document.addEventListener('DOMContentLoaded', function () {
    actualizarPaginacionAsis();
});
