package com.sistemadevoluntariado.controller;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.sistemadevoluntariado.repository.NotificacionRepository;
import com.sistemadevoluntariado.entity.Notificacion;
import com.sistemadevoluntariado.entity.Usuario;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(name = "NotificacionController", urlPatterns = { "/notificaciones" })
public class NotificacionController extends HttpServlet {

    private NotificacionRepository NotificacionRepository;
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (NotificacionRepository == null) NotificacionRepository = new NotificacionRepository();
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioLogeado") == null) {
            response.setStatus(401);
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogeado");
        int idUsuario = usuario.getIdUsuario();
        String action = request.getParameter("action");

        response.setContentType("application/json;charset=UTF-8");

        if ("listar".equals(action)) {
            List<Notificacion> lista = NotificacionRepository.listarPorUsuario(idUsuario);
            int noLeidas = NotificacionRepository.contarNoLeidas(idUsuario);
            response.getWriter().write(
                    "{\"success\":true,\"notificaciones\":" + gson.toJson(lista) + ",\"noLeidas\":" + noLeidas + "}");

        } else if ("contar".equals(action)) {
            int noLeidas = NotificacionRepository.contarNoLeidas(idUsuario);
            response.getWriter().write("{\"noLeidas\":" + noLeidas + "}");

        } else if ("marcarLeida".equals(action)) {
            int id = Integer.parseInt(request.getParameter("id"));
            NotificacionRepository.marcarLeida(id);
            response.getWriter().write("{\"success\":true}");

        } else if ("marcarTodas".equals(action)) {
            NotificacionRepository.marcarTodasLeidas(idUsuario);
            response.getWriter().write("{\"success\":true}");

        } else {
            response.getWriter().write("{\"error\":\"Acción no válida\"}");
        }
    }
}
