package com.sistemadevoluntariado.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
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
    // Guardar fotos directamente en la carpeta img/
    private static final String UPLOAD_DIR = "img";

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

            // Ruta de la carpeta img/ dentro del deploy
            String appPath = request.getServletContext().getRealPath("");
            String uploadPath = appPath + File.separator + UPLOAD_DIR;
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
                logger.info("► Directorio img/ creado: " + uploadPath);
            }

            // ────────────────────────────────────────────────────
            // PRIVACIDAD: Eliminar TODAS las fotos anteriores
            // del usuario antes de guardar la nueva.
            // Busca cualquier archivo que empiece con "perfil_<id>"
            // ────────────────────────────────────────────────────
            String prefijo = "perfil_" + usuario.getIdUsuario();
            File[] fotosAnteriores = uploadDir.listFiles((dir, name) -> name.startsWith(prefijo));
            if (fotosAnteriores != null) {
                for (File fotoVieja : fotosAnteriores) {
                    if (fotoVieja.delete()) {
                        logger.info("► Foto anterior eliminada (privacidad): " + fotoVieja.getName());
                    }
                }
            }

            // También borrar desde la ruta guardada en BD (por si estaba en uploads/fotos)
            if (usuario.getFotoPerfil() != null && !usuario.getFotoPerfil().isEmpty()) {
                File fotoAnteriorBD = new File(appPath + File.separator + usuario.getFotoPerfil().replace("/", File.separator));
                if (fotoAnteriorBD.exists()) {
                    fotoAnteriorBD.delete();
                    logger.info("► Foto anterior (ruta BD) eliminada: " + fotoAnteriorBD.getAbsolutePath());
                }
            }

            // Nombre fijo por usuario: perfil_<id>.<ext> (sin UUID = una sola foto siempre)
            String originalFileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            String extension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
            String nuevoNombre = prefijo + extension;

            // Guardar archivo en img/
            filePart.write(uploadPath + File.separator + nuevoNombre);
            logger.info("✓ Foto guardada en img/: " + nuevoNombre);

            // Ruta relativa para la BD
            String rutaRelativa = UPLOAD_DIR + "/" + nuevoNombre;

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
