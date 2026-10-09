package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.application.BookingViews.AppointmentView;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Arma los turnos del historial de una cuenta dentro de cada negocio, con el aislamiento normal. */
@Component
class AccountHistoryReader {

    private final AppointmentRepository appointments;
    private final AppointmentViewer viewer;

    AccountHistoryReader(AppointmentRepository appointments, AppointmentViewer viewer) {
        this.appointments = appointments;
        this.viewer = viewer;
    }

    /** Son turnos de la propia persona: ve sus datos de contacto. */
    @BusinessScoped
    @Transactional(readOnly = true)
    public List<AppointmentView> views(@BusinessId UUID businessId, Collection<UUID> appointmentIds) {
        return viewer.views(businessId, appointments.findAllByIdIn(appointmentIds), own -> true);
    }
}
