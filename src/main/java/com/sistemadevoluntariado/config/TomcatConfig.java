package com.sistemadevoluntariado.config;

import java.io.File;
import java.net.URL;

import org.apache.catalina.webresources.DirResourceSet;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra la carpeta resources/templates/ como raíz de documentos web
 * para que getRequestDispatcher("/views/...") y forward:/ encuentren las vistas.
 *
 * De esta forma las vistas viven en src/main/resources/templates/
 * y no se necesita la carpeta webapp/.
 */
@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> templatesAsWebRoot() {
        return factory -> factory.addContextCustomizers(context -> {
            try {
                URL resource = TomcatConfig.class.getClassLoader().getResource("templates/");
                if (resource != null) {
                    String path;
                    if ("file".equals(resource.getProtocol())) {
                        path = new File(resource.toURI()).getAbsolutePath();
                    } else {
                        // Fallback: buscar en target/classes/templates/
                        path = new File("target/classes/templates").getAbsolutePath();
                    }
                    System.out.println("[TomcatConfig] Registrando web root: " + path);
                    if (new File(path).isDirectory()) {
                        context.getResources().addPreResources(
                                new DirResourceSet(context.getResources(), "/", path, "/"));
                        System.out.println("[TomcatConfig] Web root registrado OK");

                        // Registrar el JSP servlet para que procese también archivos .html
                        context.addServletMappingDecoded("*.html", "jsp");
                        System.out.println("[TomcatConfig] JSP servlet mapeado a *.html");
                    } else {
                        System.err.println("[TomcatConfig] La ruta NO es un directorio: " + path);
                    }
                } else {
                    System.err.println("[TomcatConfig] No se encontró templates/ en el classpath");
                }
            } catch (Exception e) {
                System.err.println("[TomcatConfig] Error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
