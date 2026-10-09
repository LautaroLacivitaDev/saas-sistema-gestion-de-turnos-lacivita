package com.lacivita.turnos.notifications.application;

import com.lacivita.turnos.booking.AppointmentDirectory;
import com.lacivita.turnos.notifications.application.NotificationViews.DeliveryView;
import com.lacivita.turnos.notifications.domain.NotificationRepository;
import com.lacivita.turnos.notifications.domain.UnknownAppointmentException;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro de envíos de un turno, para su ficha: qué avisos salieron, cuáles esperan y cuáles fallaron. */
@Service
public class DeliveryLog {

    private final NotificationRepository notifications;
    private final AppointmentDirectory appointments;

    DeliveryLog(NotificationRepository notifications, AppointmentDirectory appointments) {
        this.notifications = notifications;
        this.appointments = appointments;
    }

    /** Lo ve quien puede ver el turno en la agenda. */
    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public List<DeliveryView> of(@BusinessId UUID businessId, AuthenticatedUser actor, UUID appointmentId) {
        if (!appointments.isVisibleTo(businessId, actor, appointmentId)) {
            throw new UnknownAppointmentException();
        }
        return notifications.findByAppointment(appointmentId).stream()
                .map(DeliveryView::of)
                .toList();
    }
}
