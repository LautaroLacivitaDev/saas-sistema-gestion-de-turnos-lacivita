package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.application.BookingViews.AppointmentView;
import com.lacivita.turnos.booking.domain.Appointment;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.AppointmentStatus;
import com.lacivita.turnos.booking.domain.BookingPolicy;
import com.lacivita.turnos.booking.domain.Customer;
import com.lacivita.turnos.booking.domain.CustomerNotFoundException;
import com.lacivita.turnos.booking.domain.CustomerRepository;
import com.lacivita.turnos.booking.domain.NotOfferedException;
import com.lacivita.turnos.catalog.BookableItem;
import com.lacivita.turnos.catalog.ServiceQuotes;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.TimeInterval;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agenda del equipo: ver los turnos, cargar turnos de clientes que llaman o llegan sin reserva, cambiar
 * su estado y moverlos. Cada persona trabaja en sus sucursales; el dueño, en todas.
 */
// DECISIÓN: los turnos que carga el equipo no se limitan al horario de trabajo ni a la anticipación (el
// equipo decide, por ejemplo, atender a alguien fuera de hora). Lo que nunca se permite es superponer dos
// turnos del mismo profesional.
@Service
public class Agenda {

    /** Un pedido de agenda abarca como mucho un mes: la vista es por día o por semana. */
    static final Duration MAX_PERIOD = Duration.ofDays(31);

    private final AppointmentRepository appointments;
    private final CustomerRepository customerRepository;
    private final Customers customers;
    private final AppointmentSlots slots;
    private final ServiceQuotes quotes;
    private final BookingActors actors;
    private final AppointmentViewer viewer;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    Agenda(
            AppointmentRepository appointments,
            CustomerRepository customerRepository,
            Customers customers,
            AppointmentSlots slots,
            ServiceQuotes quotes,
            BookingActors actors,
            AppointmentViewer viewer,
            ApplicationEventPublisher events,
            Clock clock) {
        this.appointments = appointments;
        this.customerRepository = customerRepository;
        this.customers = customers;
        this.slots = slots;
        this.quotes = quotes;
        this.actors = actors;
        this.viewer = viewer;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Turnos del período que la persona puede ver, con filtros opcionales.
     *
     * @param branchId una sucursal; {@code null} para todas las que puede ver
     * @param barberId un profesional; {@code null} para todos
     */
    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public List<AppointmentView> of(
            @BusinessId UUID businessId, AuthenticatedUser actor, TimeInterval period, UUID branchId, UUID barberId) {
        if (period.length().compareTo(MAX_PERIOD) > 0) {
            throw new InvalidValueException("invalid_period", "Elegí un período de hasta un mes.");
        }
        var membership = actors.actorIn(actor, businessId);
        var visible = appointments.findInPeriod(AppointmentStatus.IN_AGENDA, period.start(), period.end()).stream()
                .filter(appointment -> BookingPolicy.canSee(membership, actor.id(), appointment))
                .filter(appointment ->
                        branchId == null || appointment.getBranchId().equals(branchId))
                .filter(appointment ->
                        barberId == null || appointment.getBarberId().equals(barberId))
                .toList();
        return viewer.views(
                businessId, visible, appointment -> BookingPolicy.canSeeContact(membership, actor.id(), appointment));
    }

    /** Carga un turno de un cliente que llamó o llegó sin reserva. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public AppointmentView book(@BusinessId UUID businessId, AuthenticatedUser actor, CounterBooking booking) {
        BookingPolicy.checkCanManage(actors.actorIn(actor, businessId), booking.branchId());
        actors.branchOf(businessId, booking.branchId());
        actors.barberAt(businessId, booking.barberId(), booking.branchId());
        var quote = quotes.quote(businessId, booking.barberId(), booking.item()).orElseThrow(NotOfferedException::new);
        var now = clock.instant();
        var customer = booking.customerId() != null
                ? customerRepository.findById(booking.customerId()).orElseThrow(CustomerNotFoundException::new)
                : customers.forContact(businessId, booking.contact(), now);

        var appointment = slots.take(
                Appointment.atCounter(
                        businessId,
                        booking.branchId(),
                        booking.barberId(),
                        customer.getId(),
                        booking.item(),
                        OnlineBooking.lines(quote),
                        booking.start(),
                        booking.confirmed(),
                        actor.id(),
                        now),
                now);
        events.publishEvent(new BookingEvents.AppointmentBooked(
                businessId,
                appointment.getId(),
                appointment.getBarberId(),
                appointment.getStartsAt(),
                appointment.getSource().name(),
                appointment.getStatus().name()));
        return viewer.view(businessId, appointment);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public AppointmentView changeStatus(
            @BusinessId UUID businessId, AuthenticatedUser actor, UUID appointmentId, AppointmentStatus target) {
        var appointment = appointments.require(appointmentId);
        BookingPolicy.checkCanManage(actors.actorIn(actor, businessId), appointment.getBranchId());
        var before = appointment.getStatus();
        var now = clock.instant();
        switch (target) {
            case CONFIRMED -> appointment.confirm(now);
            case IN_PROGRESS -> appointment.start(now);
            case COMPLETED -> appointment.complete(now);
            case NO_SHOW -> appointment.markNoShow(now);
            case CANCELLED -> appointment.cancel(now);
            case HOLD, PENDING ->
                throw new InvalidValueException("invalid_status", "Un turno no puede volver a ese estado.");
        }
        events.publishEvent(new BookingEvents.StatusChanged(businessId, appointmentId, before, target));
        return viewer.view(businessId, appointment);
    }

    /**
     * Mueve un turno a otro horario y, si se indica, a otro profesional de la misma sucursal. Conserva los
     * servicios y el precio con que se reservó.
     */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public AppointmentView reschedule(
            @BusinessId UUID businessId,
            AuthenticatedUser actor,
            UUID appointmentId,
            Instant newStart,
            UUID newBarberId) {
        var appointment = appointments.require(appointmentId);
        BookingPolicy.checkCanManage(actors.actorIn(actor, businessId), appointment.getBranchId());
        var barberBefore = appointment.getBarberId();
        var startBefore = appointment.getStartsAt();
        var barberAfter = newBarberId == null ? barberBefore : newBarberId;
        actors.barberAt(businessId, barberAfter, appointment.getBranchId());

        var now = clock.instant();
        slots.releaseExpiredHolds(barberAfter, now);
        appointment.reschedule(newStart, barberAfter, now);
        slots.flushMove();
        events.publishEvent(new BookingEvents.Rescheduled(
                businessId, appointmentId, barberBefore, startBefore, barberAfter, newStart));
        return viewer.view(businessId, appointment);
    }

    /**
     * Turno cargado por el equipo.
     *
     * @param customerId un cliente ya cargado; si es {@code null}, se usa {@code contact}
     * @param confirmed {@code false} lo deja "a confirmar" con el cliente
     */
    public record CounterBooking(
            UUID branchId,
            UUID barberId,
            BookableItem item,
            Instant start,
            UUID customerId,
            Customer.Contact contact,
            boolean confirmed) {

        public CounterBooking {
            if (customerId == null && contact == null) {
                throw new InvalidValueException("customer_required", "Elegí un cliente o cargá sus datos.");
            }
        }
    }
}
