<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>  
<%@ page import="com.sistemadevoluntariado.model.Usuario" %>

<%
    Usuario usuario = (Usuario) request.getAttribute("usuario");
    if (usuario == null) {
        response.sendRedirect(request.getContextPath() + "/login");
        return;
    }
%>

<!DOCTYPE html>
<html lang="es">

<head>
    <meta charset="UTF-8">
    <title>Dashboard - Sistema de Voluntariado</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/dashboard.css">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>

<body>

<div class="dashboard-container">

    <jsp:include page="/includes/sidebar.jsp" />

    <main class="main-content">
        <jsp:include page="/includes/topbar.jsp" />

        <!-- TARJETAS / STAT CARDS -->
        <section class="stats">
            
            <div class="card">
                <div class="icon purple">
                    <i class="fas fa-user-friends"></i>
                </div>
                <div class="text">
                    <h2>${voluntariosActivos}</h2>
                    <p>Voluntarios Activos</p>
                </div>
            </div>

            <div class="card">
                <div class="icon blue">
                    <i class="fas fa-calendar-check"></i>
                </div>
                <div class="text">
                    <h2>${totalActividades}</h2>
                    <p>Actividades</p>
                </div>
            </div>

            <div class="card">
                <div class="icon green">
                    <i class="fas fa-hand-holding-heart"></i>
                </div>
                <div class="text">
                    <h2>${totalDonaciones}</h2>
                    <p>Donaciones</p>
                </div>
            </div>

            <div class="card">
                <div class="icon yellow">
                    <i class="fas fa-users"></i>
                </div>
                <div class="text">
                    <h2>${totalBeneficiarios}</h2>
                    <p>Beneficiarios</p>
                </div>
            </div>

        </section>

        <!-- ============================================= -->
        <!-- SECCIÓN: Donaciones + Gráficos                -->
        <!-- ============================================= -->
        <section class="dashboard-middle">

            <!-- COLUMNA IZQUIERDA -->
            <div class="middle-left">

                <!-- CARD DONACIONES (ancha) -->
                <div class="card card-donaciones">
                    <div class="donacion-content">
                        <div>
                            <div class="donacion-monto">
                                <i class="fas fa-money-bill-wave"></i> S/ ${montoDonaciones}
                            </div>
                            <div class="donacion-titulo">Total Donaciones</div>
                        </div>
                        <div class="donacion-icon">
                            <img src="${pageContext.request.contextPath}/img/money.png" alt="Donaciones" onerror="this.style.display='none'">
                            <i class="fas fa-money-bill-wave fa-3x" style="color:rgba(255,255,255,0.3)"></i>
                        </div>
                    </div>
                </div>

                <!-- GRÁFICO LÍNEA: Actividades por Mes -->
                <div class="chart-card">
                    <div class="chart-header">
                        <h3 class="chart-title">Actividades por Mes</h3>
                        <span class="chart-badge blue">
                            <i class="fas fa-chart-line"></i> Tendencia
                        </span>
                    </div>
                    <div class="chart-body">
                        <canvas id="chartActividadesMes"></canvas>
                    </div>
                </div>

            </div>

            <!-- COLUMNA DERECHA: Donut Horas -->
            <div class="chart-card chart-card-right">
                <div class="chart-header">
                    <h3 class="chart-title">Actividades por Mes</h3>
                </div>
                <div class="chart-body chart-body-donut">
                    <div class="donut-chart-area">
                        <canvas id="chartActividadesMesRight"></canvas>
                    </div>
                    <div class="donut-big-wrapper">
                        <canvas id="chartHorasVoluntarias"></canvas>
                        <div class="donut-big-center">
                            <span class="donut-big-total">${totalHorasVoluntarias}h</span>
                            <span class="donut-big-label">Total Horas</span>
                        </div>
                    </div>
                </div>
                <div class="chart-footer">
                    <div class="proxima-actividad">
                        <div class="pa-icon"><i class="fas fa-clipboard-list"></i></div>
                        <div class="pa-info">
                            <span class="pa-label">Próxima Actividad</span>
                            <span class="pa-name">${proximaActividadNombre != null ? proximaActividadNombre : 'Sin actividades próximas'}</span>
                        </div>
                    </div>
                    <a href="${pageContext.request.contextPath}/actividades" class="btn-ver green">
                        Ver Actividades <i class="fas fa-arrow-right"></i>
                    </a>
                </div>
            </div>

        </section>

        

    </main>

</div>

<script src="https://kit.fontawesome.com/a2e0e6ad65.js" crossorigin="anonymous"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
<script src="${pageContext.request.contextPath}/js/graficos.js"></script>

<script>
    // Solo pasamos los datos del servidor al JS externo
    initDashboardCharts({
        actLabels:   ${actividadesPorMesLabels != null ? actividadesPorMesLabels : '["Ene","Feb","Mar","Abr","May","Jun"]'},
        actData:     ${actividadesPorMesData != null ? actividadesPorMesData : '[0,0,0,0,0,0]'},
        horasLabels: ${horasLabels != null ? horasLabels : '[]'},
        horasData:   ${horasData != null ? horasData : '[]'}
    });
</script>

</body>

</html>