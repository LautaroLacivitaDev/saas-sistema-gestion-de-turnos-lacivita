package com.lacivita.turnos;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reloj de las pruebas de integración: anda como el real, pero se puede adelantar para probar lo que
 * depende del paso del tiempo (recordatorios, reintentos). Como el contexto es compartido, cada prueba
 * que lo adelanta lo vuelve a la hora real al terminar ({@link #reset()}).
 */
public class TestClock extends Clock {

    private final AtomicReference<Duration> offset = new AtomicReference<>(Duration.ZERO);

    public void advance(Duration duration) {
        offset.updateAndGet(current -> current.plus(duration));
    }

    /** Lleva el reloj a ese momento (solo hacia adelante tiene sentido). */
    public void jumpTo(Instant instant) {
        offset.set(Duration.between(Instant.now(), instant));
    }

    public void reset() {
        offset.set(Duration.ZERO);
    }

    @Override
    public Instant instant() {
        return Instant.now().plus(offset.get());
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("La aplicación usa el reloj en UTC");
    }
}
