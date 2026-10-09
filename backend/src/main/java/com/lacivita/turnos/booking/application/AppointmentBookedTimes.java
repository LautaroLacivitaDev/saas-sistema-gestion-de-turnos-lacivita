package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.AppointmentStatus;
import com.lacivita.turnos.schedule.BookedTime;
import com.lacivita.turnos.schedule.BookedTimes;
import com.lacivita.turnos.shared.domain.TimeInterval;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Informa a la agenda los turnos que ocupan a cada profesional, para que no los ofrezca. */
@Component
class AppointmentBookedTimes implements BookedTimes {

    private final AppointmentRepository appointments;
    private final Clock clock;

    AppointmentBookedTimes(AppointmentRepository appointments, Clock clock) {
        this.appointments = appointments;
        this.clock = clock;
    }

    @Override
    @BusinessScoped
    @Transactional(readOnly = true)
    public List<BookedTime> of(@BusinessId UUID businessId, UUID barberId, TimeInterval period) {
        return appointments
                .findOccupying(barberId, AppointmentStatus.OCCUPYING, period.start(), period.end(), clock.instant())
                .stream()
                .map(appointment -> new BookedTime(appointment.getId(), appointment.interval()))
                .toList();
    }
}
