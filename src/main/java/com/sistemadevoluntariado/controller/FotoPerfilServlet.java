package com.sistemadevoluntariado.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import com.sistemadevoluntariado.dao.UsuarioDAO;
import com.sistemadevoluntariado.model.Usuario;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@WebServlet("/foto-perfil")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,      // 1 MB
    maxFileSize       = 5 * 1024 * 1024,   // 5 MB
    maxRequestSize    = 10 * 1024 * 1024    // 10 MB
)
public class FotoPerfilServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(FotoPerfilServlet.class.getName());
    private static final String UPLOAD_DIR = "uploads" + File.separator + "fotos";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        JsonObject json = new JsonObject();

        try {
            // Verificar sesión
            Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogeado");
            if (usuario == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                json.addProperty("success", false);
                json.addProperty("message", "Sesión expirada. Inicie sesión nuevamente.");
                try (PrintWriter out = response.getWriter()) {
                    out.print(json.toString());
                }
                return;
            }

            // Obtener el archivo
            Part filePart = request.getPart("foto");
            if (filePart == null || filePart.getSize() == 0) {
                json.addProperty("success", false);
                json.addProperty("message", "No se seleccionó ningún archivo.");
                try (PrintWriter out = response.getWriter()) {
                    out.print(json.toString());
                }
                return;
            }

            // Validar tipo de archivo
            String contentType = filePart.getContentType();
            if (!contentType.startsWith("image/")) {
                json.addProperty("success", false);
                json.addProperty("message", "Solo se permiten archivos de imagen (JPG, PNG, GIF).");
                try (PrintWriter out = response.getWriter()) {
                    out.print(json.toString());
                }
                return;
            }

            // Crear directorio de uploads si no existe
            String appPath = request.getServletContext().getRealPath("");
            String uploadPath = appPath + File.separator + UPLOAD_DIR;
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
                logger.info("► Directorio de uploads creado: " + uploadPath);
            }

            // Generar nombre único para el archivo
            String originalFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String nuevoNombre = "perfil_" + usuario.getIdUsuario() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            // Eliminar foto anterior si existe
            if (usuario.getFotoPerfil() != null && !usuario.getFotoPerfil().isEmpty()) {
                File fotoAnterior = new File(appPath + File.separator + usuario.getFotoPerfil().replace("/", File.separator));
                if (fotoAnterior.exists()) {
                    fotoAnterior.delete();
                    logger.info("► Foto anterior eliminada: " + fotoAnterior.getAbsolutePath());
                }
            }

            // Guardar archivo
            filePart.write(uploadPath + File.separator + nuevoNombre);
            logger.info("✓ Foto guardada: " + uploadPath + File.separator + nuevoNombre);

            // Ruta relativa para guardar en BD
            String rutaRelativa = UPLOAD_DIR.replace(File.separator, "/") + "/" + nuevoNombre;

            // Actualizar en la BD
            UsuarioDAO dao = new UsuarioDAO();
            boolean actualizado = dao.actualizarFotoPerfil(usuario.getIdUsuario(), rutaRelativa);

            if (actualizado) {
                // Actualizar la sesión
                usuario.setFotoPerfil(rutaRelativa);
                request.getSession().setAttribute("usuarioLogeado", usuario);

                json.addProperty("success", true);
                json.addProperty("message", "Foto de perfil actualizada correctamente.");
                json.addProperty("fotoUrl", request.getContextPath() + "/" + rutaRelativa);
            } else {
                json.addProperty("success", false);
                json.addProperty("message", "Error al guardar la foto en la base de datos.");
            }

        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "✗ Archivo demasiado grande", e);
            json.addProperty("success", false);
            json.addProperty("message", "El archivo es demasiado grande. Máximo 5 MB.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "✗ Error al subir foto de perfil", e);
            json.addProperty("success", false);
            json.addProperty("message", "Error inesperado al subir la foto.");
        }

        try (PrintWriter out = response.getWriter()) {
            out.print(json.toString());
        }
    }
}
