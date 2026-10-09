package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.application.BookingViews.ManagedAppointmentView;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.AppointmentStatus;
import com.lacivita.turnos.booking.domain.BookingSettingsRepository;
import com.lacivita.turnos.booking.domain.ManageToken;
import com.lacivita.turnos.booking.domain.SlotNotAvailableException;
import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.business.BusinessSummary;
import com.lacivita.turnos.schedule.FreeBarbers;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que el cliente hace con el link de su turno, sin iniciar sesión: verlo, cancelarlo o reprogramarlo,
 * dentro del plazo del negocio.
 */
@Service
class CustomerChanges {

    private final AppointmentRepository appointments;
    private final BookingSettingsRepository settings;
    private final AppointmentSlots slots;
    private final FreeBarbers freeBarbers;
    private final BusinessDirectory businesses;
    private final AppointmentViewer viewer;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    CustomerChanges(
            AppointmentRepository appointments,
            BookingSettingsRepository settings,
            AppointmentSlots slots,
            FreeBarbers freeBarbers,
            BusinessDirectory businesses,
            AppointmentViewer viewer,
            ApplicationEventPublisher events,
            Clock clock) {
        this.appointments = appointments;
        this.settings = settings;
        this.slots = slots;
        this.freeBarbers = freeBarbers;
        this.businesses = businesses;
        this.viewer = viewer;
        this.events = events;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    public ManagedAppointmentView view(@BusinessId UUID businessId, ManageToken token) {
        var appointment = appointments.requireByToken(token);
        var notice = Duration.ofHours(settings.cancellationOf(businessId).noticeHours());
        return new ManagedAppointmentView(
                businesses.find(businessId).map(BusinessSummary::name).orElse(""),
                viewer.view(businessId, appointment),
                appointment.getStartsAt().minus(notice));
    }

    @BusinessScoped
    @Transactional
    public ManagedAppointmentView cancel(@BusinessId UUID businessId, ManageToken token) {
        var appointment = appointments.requireByToken(token);
        var before = appointment.getStatus();
        appointment.cancelByCustomer(settings.cancellationOf(businessId), clock.instant());
        events.publishEvent(
                new BookingEvents.StatusChanged(businessId, appointment.getId(), before, AppointmentStatus.CANCELLED));
        return view(businessId, token);
    }

    /** Mueve el turno a otro horario libre del mismo profesional. */
    @BusinessScoped
    @Transactional
    public ManagedAppointmentView reschedule(@BusinessId UUID businessId, ManageToken token, Instant newStart) {
        var appointment = appointments.requireByToken(token);
        var barberId = appointment.getBarberId();
        var free = freeBarbers.at(
                businessId, appointment.getBranchId(), appointment.item(), barberId, newStart, appointment.getId());
        if (!free.contains(barberId)) {
            throw new SlotNotAvailableException();
        }
        var now = clock.instant();
        var startBefore = appointment.getStartsAt();
        slots.releaseExpiredHolds(barberId, now);
        appointment.rescheduleByCustomer(settings.cancellationOf(businessId), newStart, now);
        slots.flushMove();
        events.publishEvent(new BookingEvents.Rescheduled(
                businessId, appointment.getId(), barberId, startBefore, barberId, newStart));
        return view(businessId, token);
    }
}
