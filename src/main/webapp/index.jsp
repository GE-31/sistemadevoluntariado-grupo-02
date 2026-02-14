<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    // Si ya está autenticado (sesión HTTP o cookie JWT), redirigir al dashboard
    if (request.getSession(false) != null && request.getSession().getAttribute("usuarioLogeado") != null) {
        response.sendRedirect(request.getContextPath() + "/dashboard");
        return;
    }
    
    // Verificar si hay cookie JWT válida
    jakarta.servlet.http.Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        for (jakarta.servlet.http.Cookie cookie : cookies) {
            if ("AUTH_TOKEN".equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }
        }
    }
%>
<%@ include file="/views/auth/login.jsp" %>
