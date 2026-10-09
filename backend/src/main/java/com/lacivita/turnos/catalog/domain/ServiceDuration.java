package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.time.Duration;

/**
 * Cuánto dura un servicio, en minutos. Va de a 5 minutos para que la agenda arme horarios prolijos.
 *
 * <p>Un combo suma varias duraciones, por eso {@link #plus} puede superar el máximo de un servicio solo.
 */
public record ServiceDuration(int minutes) implements Comparable<ServiceDuration> {

    static final int STEP = 5;
    static final int MAX_FOR_ONE_SERVICE = 480;

    public ServiceDuration {
        if (minutes <= 0 || minutes % STEP != 0) {
            throw new InvalidValueException(
                    "invalid_duration", "La duración tiene que ser de a " + STEP + " minutos (5, 10, 15...).");
        }
    }

    /** Un servicio solo dura como mucho una jornada de 8 horas (un combo puede durar más). */
    ServiceDuration requireOneService() {
        if (minutes > MAX_FOR_ONE_SERVICE) {
            throw new InvalidValueException("invalid_duration", "Un servicio puede durar hasta 8 horas.");
        }
        return this;
    }

    public ServiceDuration plus(ServiceDuration other) {
        return new ServiceDuration(minutes + other.minutes);
    }

    public Duration toDuration() {
        return Duration.ofMinutes(minutes);
    }

    @Override
    public int compareTo(ServiceDuration other) {
        return Integer.compare(minutes, other.minutes);
    }
}
