package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.application.BookingViews.ManagedAppointmentView;
import com.lacivita.turnos.booking.domain.AppointmentNotFoundException;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.ManageToken;
import com.lacivita.turnos.shared.tenancy.TenantContext;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Link del cliente para gestionar su turno. Primero averigua, como operación de sistema, a qué negocio
 * pertenece el link (es lo único que se lee sin aislamiento); después opera dentro de ese negocio.
 */
@Service
public class ManageLinks {

    private final AppointmentRepository appointments;
    private final CustomerChanges changes;

    ManageLinks(AppointmentRepository appointments, CustomerChanges changes) {
        this.appointments = appointments;
        this.changes = changes;
    }

    public ManagedAppointmentView view(String rawToken) {
        var token = new ManageToken(rawToken);
        return changes.view(businessOf(token), token);
    }

    public ManagedAppointmentView cancel(String rawToken) {
        var token = new ManageToken(rawToken);
        return changes.cancel(businessOf(token), token);
    }

    public ManagedAppointmentView reschedule(String rawToken, Instant newStart) {
        var token = new ManageToken(rawToken);
        return changes.reschedule(businessOf(token), token, newStart);
    }

    private UUID businessOf(ManageToken token) {
        return TenantContext.callAsSystem(
                        "link del cliente: averiguar el negocio del turno",
                        () -> appointments.findBusinessIdByManageTokenHash(token.hash()))
                .orElseThrow(AppointmentNotFoundException::new);
    }
}
