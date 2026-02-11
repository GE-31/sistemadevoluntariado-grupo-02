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

             <!-- CARD ESPECIAL: TOTAL DONACIONES -->
            <div class="card card-donaciones">
                
                <!-- MONTO ARRIBA -->
                <div class="donacion-monto">
                    S/ ${montoDonaciones}
                </div>

                <!-- TEXTO CENTRAL -->
                <div class="donacion-titulo">
                    Total Donaciones
                </div>

                <!-- ICONO ABAJO -->
                <div class="donacion-icon">
                    <i class="fas fa-money-bill-wave"></i>
                </div>
            </div>


        </section>

        

    </main>

</div>

<script src="https://kit.fontawesome.com/a2e0e6ad65.js" crossorigin="anonymous"></script>

</body>

</html>
