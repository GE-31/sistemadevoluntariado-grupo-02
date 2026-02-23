package com.sistemadevoluntariado.controller;
 
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.sistemadevoluntariado.repository.ActividadRepository;
import com.sistemadevoluntariado.repository.BeneficiarioRepository;
import com.sistemadevoluntariado.repository.DashboardRepository;
import com.sistemadevoluntariado.repository.DonacionRepository;
import com.sistemadevoluntariado.repository.VoluntarioRepository;
import com.sistemadevoluntariado.entity.Actividad;
import com.sistemadevoluntariado.entity.Beneficiario;
import com.sistemadevoluntariado.entity.Donacion;
import com.sistemadevoluntariado.entity.Usuario;
import com.sistemadevoluntariado.entity.Voluntario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/dashboard")
public class DashboardController extends HttpServlet {

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
        VoluntarioRepository voluntarioRepository = new VoluntarioRepository();
        ActividadRepository actividadRepository = new ActividadRepository();
        DonacionRepository donacionRepository = new DonacionRepository();
        BeneficiarioRepository beneficiarioRepository = new BeneficiarioRepository();

        // Obtener todos los voluntarios
        List<Voluntario> todosVoluntarios = voluntarioRepository.obtenerTodosVoluntarios();

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
        List<Actividad> actividades = actividadRepository.obtenerTodasActividades();
        int totalActividades = actividades.size();

        // Obtener donaciones y calcular monto total (solo contabilizar CONFIRMADO)
        List<Donacion> donaciones = donacionRepository.listarTodos();
        int totalDonaciones = 0;
        double montoDonaciones = 0;
        for (Donacion d : donaciones) {
            String est = d.getEstado() != null ? d.getEstado().toUpperCase() : "";
            if ("CONFIRMADO".equals(est) || "ACTIVO".equals(est)) { // 'ACTIVO' se trata como legado = CONFIRMADO
                totalDonaciones++;
                if ("DINERO".equals(d.getTipoDonacion())) {
                    montoDonaciones += d.getCantidad() != null ? d.getCantidad() : 0d;
                }
            }
        }

        // Obtener beneficiarios
        List<Beneficiario> beneficiarios = beneficiarioRepository.obtenerTodosBeneficiarios();
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
        DashboardRepository dashboardRepository = new DashboardRepository();
        Gson gson = new Gson();

        // Actividades por mes (últimos 6 meses)
        Map<String, Object> actividadesPorMes = dashboardRepository.obtenerActividadesPorMes();
        request.setAttribute("actividadesPorMesLabels", gson.toJson(actividadesPorMes.get("labels")));
        request.setAttribute("actividadesPorMesData", gson.toJson(actividadesPorMes.get("data")));

        // Horas voluntarias por actividad (donut chart)
        Map<String, Object> horasVoluntarias = dashboardRepository.obtenerHorasVoluntariasPorActividad();
        request.setAttribute("horasLabels", gson.toJson(horasVoluntarias.get("labels")));
        request.setAttribute("horasData", gson.toJson(horasVoluntarias.get("data")));

        // Total horas voluntarias
        double totalHoras = dashboardRepository.obtenerTotalHorasVoluntarias();
        request.setAttribute("totalHorasVoluntarias", totalHoras);

        // Próxima actividad
        Map<String, String> proxima = dashboardRepository.obtenerProximaActividad();
        if (proxima != null) {
            request.setAttribute("proximaActividadNombre", proxima.get("nombre"));
            request.setAttribute("proximaActividadFecha", proxima.get("fecha"));
        }

        request.getRequestDispatcher("/views/dashboard/dashboard.html")
                .forward(request, response);
    }
}
