package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.sistemadevoluntariado.dao.ActividadDAO;
import com.sistemadevoluntariado.dao.BeneficiarioDAO;
import com.sistemadevoluntariado.dao.DashboardDAO;
import com.sistemadevoluntariado.dao.DonacionDAO;
import com.sistemadevoluntariado.dao.VoluntarioDAO;
import com.sistemadevoluntariado.model.Actividad;
import com.sistemadevoluntariado.model.Beneficiario;
import com.sistemadevoluntariado.model.Donacion;
import com.sistemadevoluntariado.model.Usuario;
import com.sistemadevoluntariado.model.Voluntario;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Verificar si el usuario está logueado
        if (session == null || session.getAttribute("usuarioLogeado") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogeado");
        request.setAttribute("usuario", usuario);

        // Obtener datos para las tarjetas del dashboard
        VoluntarioDAO voluntarioDAO = new VoluntarioDAO();
        ActividadDAO actividadDAO = new ActividadDAO();
        DonacionDAO donacionDAO = new DonacionDAO();
        BeneficiarioDAO beneficiarioDAO = new BeneficiarioDAO();

        // Obtener todos los voluntarios
        List<Voluntario> todosVoluntarios = voluntarioDAO.obtenerTodosVoluntarios();

        // Contar voluntarios activos
        int voluntariosActivos = 0;
        int voluntariosInactivos = 0;
        for (Voluntario v : todosVoluntarios) {
            if ("ACTIVO".equals(v.getEstado())) {
                voluntariosActivos++;
            } else {
                voluntariosInactivos++;
            }
        }

        int totalVoluntarios = todosVoluntarios.size();

        // Obtener actividades
        List<Actividad> actividades = actividadDAO.obtenerTodasActividades();
        int totalActividades = actividades.size();

        // Obtener donaciones y calcular monto total
        List<Donacion> donaciones = donacionDAO.listar();
        int totalDonaciones = donaciones.size();
        double montoDonaciones = 0;
        for (Donacion d : donaciones) {
            if ("DINERO".equals(d.getTipoDonacion())) {
                montoDonaciones += d.getCantidad();
            }
        }

        // Obtener beneficiarios
        List<Beneficiario> beneficiarios = beneficiarioDAO.obtenerTodosBeneficiarios();
        int totalBeneficiarios = beneficiarios.size();

        // Pasar datos al JSP
        request.setAttribute("totalVoluntarios", totalVoluntarios);
        request.setAttribute("voluntariosActivos", voluntariosActivos);
        request.setAttribute("voluntariosInactivos", voluntariosInactivos);
        request.setAttribute("totalActividades", totalActividades);
        request.setAttribute("totalDonaciones", totalDonaciones);
        request.setAttribute("montoDonaciones", montoDonaciones);
        request.setAttribute("totalBeneficiarios", totalBeneficiarios);

        // ── Datos para gráficos del Dashboard ──
        DashboardDAO dashboardDAO = new DashboardDAO();
        Gson gson = new Gson();

        // Actividades por mes (últimos 6 meses)
        Map<String, Object> actividadesPorMes = dashboardDAO.obtenerActividadesPorMes();
        request.setAttribute("actividadesPorMesLabels", gson.toJson(actividadesPorMes.get("labels")));
        request.setAttribute("actividadesPorMesData", gson.toJson(actividadesPorMes.get("data")));

        // Horas voluntarias por actividad (donut chart)
        Map<String, Object> horasVoluntarias = dashboardDAO.obtenerHorasVoluntariasPorActividad();
        request.setAttribute("horasLabels", gson.toJson(horasVoluntarias.get("labels")));
        request.setAttribute("horasData", gson.toJson(horasVoluntarias.get("data")));

        // Total horas voluntarias
        double totalHoras = dashboardDAO.obtenerTotalHorasVoluntarias();
        request.setAttribute("totalHorasVoluntarias", totalHoras);

        // Próxima actividad
        Map<String, String> proxima = dashboardDAO.obtenerProximaActividad();
        if (proxima != null) {
            request.setAttribute("proximaActividadNombre", proxima.get("nombre"));
            request.setAttribute("proximaActividadFecha", proxima.get("fecha"));
        }

        request.getRequestDispatcher("/views/dashboard/dashboard.jsp")
                .forward(request, response);
    }
}
