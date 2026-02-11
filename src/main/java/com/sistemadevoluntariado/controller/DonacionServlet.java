package com.sistemadevoluntariado.controller;

import java.io.IOException;

import com.sistemadevoluntariado.dao.DonacionDAO;
import com.sistemadevoluntariado.model.Donacion;
import com.sistemadevoluntariado.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/donaciones")
public class DonacionServlet extends HttpServlet {

    DonacionDAO donacionDAO = new DonacionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("page", "donaciones");
        req.setAttribute("donaciones", donacionDAO.listar());

        req.getRequestDispatcher("/views/donaciones/donaciones.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Usuario user = (Usuario) req.getSession().getAttribute("usuarioLogeado");

        Donacion d = new Donacion();
        d.setCantidad(Double.parseDouble(req.getParameter("cantidad")));
        d.setDescripcion(req.getParameter("descripcion"));
        d.setIdTipoDonacion(Integer.parseInt(req.getParameter("tipoDonacion")));
        d.setIdActividad(Integer.parseInt(req.getParameter("actividad")));
        d.setIdUsuarioRegistro(user.getIdUsuario());

        donacionDAO.guardar(d);
        resp.sendRedirect("donaciones");
    }
}
