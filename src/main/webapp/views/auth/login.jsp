<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="es">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login - Sistema de Voluntariado</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/login.css">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap"
        rel="stylesheet">
</head>

<body>
    <div class="login-background">
        <div class="login-container">
    <div class="login-card">
        <div class="login-logo">
            <img src="${pageContext.request.contextPath}/img/logo.png" alt="Logo Voluntariado">
        </div>
        <h1 class="login-title">Bienvenido</h1>
        <p class="login-subtitle">Ingresa tus credenciales para continuar</p>

        <div class="alert-error" style="<%= request.getAttribute("error") != null ? "" : "opacity: 0;" %>">
            <span><%= request.getAttribute("error") != null ? request.getAttribute("error") : "" %></span>
        </div>

        <form action="${pageContext.request.contextPath}/login" method="post" id="loginForm" class="login-form">
            <div class="form-group">
                <label for="usuario">Usuario</label>
                <input type="text" id="usuario" name="usuario" placeholder="Tu usuario" required autofocus>
            </div>

            <div class="form-group">
                <label for="clave">Contraseña</label>
                <input type="password" id="clave" name="clave" placeholder="Tu contraseña" required>
            </div>

            <div class="form-options">
                <label class="checkbox-container">Recordarme
                    <input type="checkbox" name="remember">
                    <span class="checkmark"></span>
                </label>
                <a href="#" class="forgot-password">¿Olvidaste tu contraseña?</a>
            </div>

            <button type="submit" class="btn-login">Iniciar Sesión</button>
        </form>

        <div class="login-footer">
            <p>¿No tienes una cuenta? <a href="#">Contáctanos</a></p>
        </div>
    </div>
</div>

    </div>

    <script src="${pageContext.request.contextPath}/js/login.js"></script>
</body>
</html>
