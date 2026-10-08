package com.lacivita.turnos.shared.domain;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Generador de identificadores UUID versión 7 (RFC 9562).
 *
 * <p>Los UUID v7 empiezan con la marca de tiempo, así que se insertan casi en orden en los índices de
 * PostgreSQL. Con UUID v4 (aleatorios) los índices se fragmentan a medida que crecen las tablas. Las
 * entidades generan su propio id al crearse, sin depender de la base de datos.
 */
public final class Ids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {}

    public static UUID newId() {
        return newId(Instant.now());
    }

    static UUID newId(Instant timestamp) {
        long millis = timestamp.toEpochMilli();
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);

        long mostSignificant = (millis << 16) | 0x7000L | ((random[0] & 0x0FL) << 8) | (random[1] & 0xFFL);

        long leastSignificant = 0x80L | (random[2] & 0x3FL);
        for (int i = 3; i < 10; i++) {
            leastSignificant = (leastSignificant << 8) | (random[i] & 0xFFL);
        }
        return new UUID(mostSignificant, leastSignificant);
    }
}
