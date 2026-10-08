package com.lacivita.turnos.users.domain;

/**
 * Puerto del dominio para calcular y comparar hashes de contraseñas. El dominio no conoce el algoritmo;
 * la implementación vive en la infraestructura del módulo.
 */
public interface PasswordHasher {

    String hash(NewPassword password);

    boolean matches(String rawPassword, String storedHash);

    /**
     * Hace una comparación descartable que tarda lo mismo que {@link #matches}. Se usa cuando no hay
     * contraseña contra la cual comparar, para que el tiempo de respuesta no revele si la cuenta existe
     * o si tiene contraseña.
     */
    void simulateMatch(String rawPassword);
}
