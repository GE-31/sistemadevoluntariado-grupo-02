package com.sistemadevoluntariado;

import org.mindrot.jbcrypt.BCrypt;

public class GenerarHashBCrypt {
    public static void main(String[] args) {
        // Generar hashes BCrypt para las contraseñas de prueba
        String pass1 = "admin123";
        String pass2 = "demo123";

        String hash1 = BCrypt.hashpw(pass1, BCrypt.gensalt());
        String hash2 = BCrypt.hashpw(pass2, BCrypt.gensalt());

        System.out.println("Usuario: admin");
        System.out.println("Contraseña: " + pass1);
        System.out.println("Hash: " + hash1);
        System.out.println();
        System.out.println("Usuario: demo");
        System.out.println("Contraseña: " + pass2);
        System.out.println("Hash: " + hash2);
    }
}
