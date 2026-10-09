package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import jakarta.persistence.Embeddable;
import java.time.Duration;
import java.util.Set;

/**
 * Reglas del negocio para ofrecer horarios.
 *
 * @param bufferMinutes tiempo de preparación entre un turno y el siguiente del mismo profesional
 * @param minNoticeMinutes anticipación mínima: no se ofrecen horarios que empiecen antes
 * @param maxAdvanceDays anticipación máxima: no se ofrecen días más lejanos
 * @param slotStepMinutes cada cuántos minutos se ofrece un horario (9:00, 9:15, 9:30...)
 */
@Embeddable
public record ScheduleRules(int bufferMinutes, int minNoticeMinutes, int maxAdvanceDays, int slotStepMinutes) {

    static final int MAX_BUFFER_MINUTES = 120;
    static final int MAX_NOTICE_MINUTES = 7 * 24 * 60;
    static final int MAX_ADVANCE_DAYS = 365;
    static final Set<Integer> SLOT_STEPS = Set.of(5, 10, 15, 20, 30, 60);

    // Después de los límites: el constructor los usa al crear esta instancia.
    public static final ScheduleRules DEFAULT = new ScheduleRules(0, 60, 60, 15);

    public ScheduleRules {
        if (bufferMinutes < 0 || bufferMinutes > MAX_BUFFER_MINUTES || bufferMinutes % 5 != 0) {
            throw invalid("El tiempo de preparación va de 0 a 120 minutos, de a 5.");
        }
        if (minNoticeMinutes < 0 || minNoticeMinutes > MAX_NOTICE_MINUTES) {
            throw invalid("La anticipación mínima puede ser de hasta 7 días.");
        }
        if (maxAdvanceDays < 1 || maxAdvanceDays > MAX_ADVANCE_DAYS) {
            throw invalid("La anticipación máxima va de 1 a 365 días.");
        }
        if (!SLOT_STEPS.contains(slotStepMinutes)) {
            throw invalid("Los horarios se pueden ofrecer cada 5, 10, 15, 20, 30 o 60 minutos.");
        }
    }

    public Duration buffer() {
        return Duration.ofMinutes(bufferMinutes);
    }

    public Duration minNotice() {
        return Duration.ofMinutes(minNoticeMinutes);
    }

    public Duration slotStep() {
        return Duration.ofMinutes(slotStepMinutes);
    }

    private static InvalidValueException invalid(String message) {
        return new InvalidValueException("invalid_schedule_rules", message);
    }
}
