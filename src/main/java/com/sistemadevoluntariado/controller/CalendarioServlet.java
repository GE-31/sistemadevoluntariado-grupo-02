package com.sistemadevoluntariado.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.sistemadevoluntariado.dao.CalendarioDAO;
import com.sistemadevoluntariado.model.Calendario;
import com.sistemadevoluntariado.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "CalendarioServlet", urlPatterns = {"/calendario"})
public class CalendarioServlet extends HttpServlet {

    private CalendarioDAO dao = new CalendarioDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String accion = request.getParameter("accion");

        if ("listar".equals(accion)) {
            List<Calendario> eventos = dao.listarEventos();

            // Convertir a formato FullCalendar: title, start, end, color
            List<Map<String, Object>> fcEventos = new ArrayList<>();
            for (Calendario ev : eventos) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", ev.getIdEvento());
                m.put("title", ev.getTitulo());
                m.put("start", ev.getFechaInicio());
                m.put("end", ev.getFechaFin());
                m.put("color", ev.getColor() != null ? ev.getColor() : "#6366f1");
                m.put("description", ev.getDescripcion());
                fcEventos.add(m);
            }

            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new Gson().toJson(fcEventos));
            return;
        }

        request.getRequestDispatcher("views/calendario/calendario.jsp")
                .forward(request, response);
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String accion = request.getParameter("accion");

        if ("guardar".equals(accion)) {

            HttpSession ses = request.getSession();
            Usuario u = (Usuario) ses.getAttribute("usuarioLogeado");

            BufferedReader reader = request.getReader();
            Calendario c = new Gson().fromJson(reader, Calendario.class);
            c.setIdUsuario(u.getIdUsuario());

            boolean ok = dao.crearEvento(c);

            response.setContentType("text/plain");
            response.getWriter().write(ok ? "ok" : "error");
        }
    }
}
