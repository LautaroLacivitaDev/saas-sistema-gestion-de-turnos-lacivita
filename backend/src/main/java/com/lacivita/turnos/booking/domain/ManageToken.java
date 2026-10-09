package com.lacivita.turnos.booking.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Secreto del link que recibe el cliente para ver, cancelar o reprogramar su turno sin iniciar sesión.
 * Solo existe en memoria y en el email: en la base se guarda su {@link #hash()} ({@link AppointmentLink}).
 */
public record ManageToken(String value) {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BYTES = 32;

    public ManageToken {
        if (value == null || value.isBlank()) {
            throw new AppointmentNotFoundException();
        }
    }

    public static ManageToken generate() {
        byte[] bytes = new byte[BYTES];
        RANDOM.nextBytes(bytes);
        return new ManageToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    /** SHA-256 en hexadecimal. Con 256 bits aleatorios no hace falta un hash lento. */
    public String hash() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    @Override
    public String toString() {
        return "ManageToken[***]";
    }
}
