package com.sistemadevoluntariado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.servlet.context.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class SistemadevoluntariadoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemadevoluntariadoApplication.class, args);
	}

}
