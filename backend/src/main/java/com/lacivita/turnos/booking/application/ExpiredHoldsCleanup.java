package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.shared.tenancy.TenantContext;
import java.time.Clock;
import org.jobrunr.jobs.annotations.Recurring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Borra los horarios reservados ({@code HOLD}) que vencieron sin confirmarse. Un HOLD vencido ya no ocupa
 * el horario (la disponibilidad lo ignora y se borra antes de tomar ese horario); esta limpieza evita que
 * se acumulen.
 *
 * <p>Es pública solo porque JobRunr la invoca por reflexión.
 */
@Component
public class ExpiredHoldsCleanup {

    private static final Logger log = LoggerFactory.getLogger(ExpiredHoldsCleanup.class);

    private final AppointmentRepository appointments;
    private final Clock clock;

    ExpiredHoldsCleanup(AppointmentRepository appointments, Clock clock) {
        this.appointments = appointments;
        this.clock = clock;
    }

    @Recurring(id = "booking-expired-holds-cleanup", interval = "PT10M")
    public void run() {
        int deleted = TenantContext.callAsSystem(
                "limpieza de horarios reservados que vencieron",
                () -> appointments.deleteAllExpiredHolds(clock.instant()));
        if (deleted > 0) {
            log.info("Se liberaron {} horarios reservados que vencieron", deleted);
        }
    }
}
