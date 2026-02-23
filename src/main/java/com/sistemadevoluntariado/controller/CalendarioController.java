package com.sistemadevoluntariado.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.sistemadevoluntariado.repository.CalendarioRepository;
import com.sistemadevoluntariado.entity.Calendario;
import com.sistemadevoluntariado.entity.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "CalendarioController", urlPatterns = {"/calendario"})
public class CalendarioController extends HttpServlet {

    private CalendarioRepository calendarioRepository;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (calendarioRepository == null) calendarioRepository = new CalendarioRepository();

        String accion = request.getParameter("accion");

        if ("listar".equals(accion)) {
            List<Calendario> eventos = calendarioRepository.listarEventos();

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

        request.getRequestDispatcher("/views/calendario/calendario.html")
                .forward(request, response);
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (calendarioRepository == null) calendarioRepository = new CalendarioRepository();

        String accion = request.getParameter("accion");

        if ("guardar".equals(accion)) {

            HttpSession ses = request.getSession();
            Usuario u = (Usuario) ses.getAttribute("usuarioLogeado");

            BufferedReader reader = request.getReader();
            Calendario c = new Gson().fromJson(reader, Calendario.class);
            c.setIdUsuario(u.getIdUsuario());

            boolean ok = calendarioRepository.crearEvento(c);

            response.setContentType("text/plain");
            response.getWriter().write(ok ? "ok" : "error");
        }
    }
}
