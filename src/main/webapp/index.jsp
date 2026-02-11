<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    // Si ya está autenticado, redirigir al dashboard
    if (request.getSession(false) != null && request.getSession().getAttribute("usuarioLogeado") != null) {
        response.sendRedirect(request.getContextPath() + "/dashboard");
        return;
    }
%>
<%@ include file="/views/auth/login.jsp" %>
