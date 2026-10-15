package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.domain.AppointmentLink;
import com.lacivita.turnos.booking.domain.AppointmentLinkRepository;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.ManageToken;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite links de gestión de un turno (ver, confirmar, cambiar o cancelar sin iniciar sesión). Cada uno es un
 * secreto nuevo: en la base se guarda solo su hash, así que no se puede volver a mostrar uno anterior.
 */
@Component
class ManageLinkIssuer {

    private final AppointmentRepository appointments;
    private final AppointmentLinkRepository links;
    private final Clock clock;

    ManageLinkIssuer(AppointmentRepository appointments, AppointmentLinkRepository links, Clock clock) {
        this.appointments = appointments;
        this.links = links;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional
    public ManageToken issue(@BusinessId UUID businessId, UUID appointmentId) {
        var appointment = appointments.require(appointmentId);
        var token = ManageToken.generate();
        links.save(AppointmentLink.to(appointment, token, clock.instant()));
        return token;
    }
}
