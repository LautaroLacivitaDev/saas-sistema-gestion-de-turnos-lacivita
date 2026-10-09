package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.catalog.BookableItem;
import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.domain.Money;
import com.lacivita.turnos.shared.domain.TimeInterval;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Turno de un cliente con un profesional en una sucursal.
 *
 * <p>Ciclo de vida:
 *
 * <ul>
 *   <li>Online: nace como {@code HOLD} (el horario queda reservado unos minutos) y pasa a {@code CONFIRMED}
 *       cuando el cliente completa sus datos. Un {@code HOLD} vencido ya no se puede confirmar.
 *   <li>En el local: nace {@code PENDING} (a confirmar) o {@code CONFIRMED}.
 *   <li>Después: {@code IN_PROGRESS}, {@code COMPLETED}, {@code CANCELLED} o {@code NO_SHOW}.
 * </ul>
 *
 * <p>Dos turnos del mismo profesional nunca se superponen: lo garantiza una restricción de exclusión en la
 * base, también entre dos reservas simultáneas.
 */
@Entity
@Table(name = "appointment")
public class Appointment {

    /** Cuánto tiempo queda reservado el horario mientras el cliente completa sus datos. */
    public static final Duration HOLD_TIME = Duration.ofMinutes(5);

    private static final Set<AppointmentStatus> CHANGEABLE =
            EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    private UUID branchId;

    private UUID barberId;

    /** Nulo mientras es un {@code HOLD}: todavía no se sabe quién es el cliente. */
    private UUID customerId;

    /** Combo reservado; nulo si se reservó un solo servicio. */
    private UUID comboId;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    private AppointmentSource source;

    private Instant startsAt;

    private Instant endsAt;

    private Instant holdExpiresAt;

    private Money totalPrice;

    @ElementCollection
    @CollectionTable(name = "appointment_line", joinColumns = @JoinColumn(name = "appointment_id"))
    @OrderColumn(name = "position")
    private List<AppointmentLine> lines = new ArrayList<>();

    /** Hash del link del cliente; nulo hasta que el turno se confirma online. */
    private String manageTokenHash;

    /** Persona del equipo que cargó el turno; nula si lo reservó el cliente. */
    private UUID createdBy;

    private Instant createdAt;

    private Instant updatedAt;

    /** Nulo hasta el primer guardado: así Spring Data distingue un alta (el id lo asignamos nosotros). */
    @Version
    private Long version;

    protected Appointment() {
        // Requerido por JPA.
    }

    private Appointment(
            UUID businessId,
            UUID branchId,
            UUID barberId,
            BookableItem item,
            List<AppointmentLine> lines,
            Instant start,
            AppointmentStatus status,
            AppointmentSource source,
            Instant now) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Un turno tiene al menos un servicio");
        }
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.branchId = Objects.requireNonNull(branchId, "branchId");
        this.barberId = Objects.requireNonNull(barberId, "barberId");
        this.comboId = item instanceof BookableItem.ComboItem(UUID combo) ? combo : null;
        this.lines = new ArrayList<>(lines);
        this.totalPrice = lines.stream().map(AppointmentLine::price).reduce(Money.ZERO, Money::plus);
        this.status = status;
        this.source = source;
        this.startsAt = Objects.requireNonNull(start, "start");
        this.endsAt = start.plus(duration());
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Reserva el horario unos minutos mientras el cliente completa sus datos. */
    public static Appointment hold(
            UUID businessId,
            UUID branchId,
            UUID barberId,
            BookableItem item,
            List<AppointmentLine> lines,
            Instant start,
            Instant now) {
        var appointment = new Appointment(
                businessId, branchId, barberId, item, lines, start, AppointmentStatus.HOLD, AppointmentSource.WEB, now);
        appointment.holdExpiresAt = now.plus(HOLD_TIME);
        return appointment;
    }

    /** Turno que carga el equipo, confirmado o a confirmar con el cliente. */
    public static Appointment atCounter(
            UUID businessId,
            UUID branchId,
            UUID barberId,
            UUID customerId,
            BookableItem item,
            List<AppointmentLine> lines,
            Instant start,
            boolean confirmed,
            UUID createdBy,
            Instant now) {
        var appointment = new Appointment(
                businessId,
                branchId,
                barberId,
                item,
                lines,
                start,
                confirmed ? AppointmentStatus.CONFIRMED : AppointmentStatus.PENDING,
                AppointmentSource.COUNTER,
                now);
        appointment.customerId = Objects.requireNonNull(customerId, "customerId");
        appointment.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        return appointment;
    }

    /** El cliente completó sus datos a tiempo: el turno queda confirmado y le llega su link. */
    public void confirmHold(UUID customer, ManageToken token, Instant now) {
        requireLiveHold(now);
        this.customerId = Objects.requireNonNull(customer, "customer");
        this.manageTokenHash = token.hash();
        this.holdExpiresAt = null;
        changeStatus(AppointmentStatus.CONFIRMED, now);
    }

    /** {@code true} si es un {@code HOLD} que todavía no venció. */
    public boolean isLiveHold(Instant now) {
        return status == AppointmentStatus.HOLD && now.isBefore(holdExpiresAt);
    }

    public void requireLiveHold(Instant now) {
        if (!isLiveHold(now)) {
            throw new HoldExpiredException();
        }
    }

    /** Un turno cargado como "a confirmar" pasa a confirmado. */
    public void confirm(Instant now) {
        requireStatus(EnumSet.of(AppointmentStatus.PENDING));
        changeStatus(AppointmentStatus.CONFIRMED, now);
    }

    public void start(Instant now) {
        requireStatus(EnumSet.of(AppointmentStatus.CONFIRMED));
        changeStatus(AppointmentStatus.IN_PROGRESS, now);
    }

    public void complete(Instant now) {
        requireStatus(EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS));
        requireStarted(now);
        changeStatus(AppointmentStatus.COMPLETED, now);
    }

    /** El cliente no vino. Solo se puede marcar una vez que pasó la hora del turno. */
    public void markNoShow(Instant now) {
        requireStatus(CHANGEABLE);
        requireStarted(now);
        changeStatus(AppointmentStatus.NO_SHOW, now);
    }

    /** Cancelación del equipo: no tiene plazo. */
    public void cancel(Instant now) {
        requireStatus(CHANGEABLE);
        changeStatus(AppointmentStatus.CANCELLED, now);
    }

    /** Cancelación del cliente desde su link: respeta el plazo del negocio. */
    public void cancelByCustomer(CancellationPolicy policy, Instant now) {
        requireCustomerCanChange(policy, now);
        cancel(now);
    }

    /** Mueve el turno, con el mismo profesional o con otro. Conserva los servicios y el precio. */
    public void reschedule(Instant newStart, UUID newBarberId, Instant now) {
        requireStatus(CHANGEABLE);
        this.barberId = Objects.requireNonNull(newBarberId, "newBarberId");
        this.startsAt = Objects.requireNonNull(newStart, "newStart");
        this.endsAt = newStart.plus(duration());
        this.updatedAt = now;
    }

    /** Reprogramación del cliente desde su link: respeta el plazo y sigue con el mismo profesional. */
    public void rescheduleByCustomer(CancellationPolicy policy, Instant newStart, Instant now) {
        requireCustomerCanChange(policy, now);
        reschedule(newStart, barberId, now);
    }

    /** Lo que se reservó: el combo o el único servicio. */
    public BookableItem item() {
        return comboId != null
                ? new BookableItem.ComboItem(comboId)
                : new BookableItem.ServiceItem(lines.getFirst().serviceId());
    }

    public Duration duration() {
        return lines.stream().map(AppointmentLine::duration).reduce(Duration.ZERO, Duration::plus);
    }

    public TimeInterval interval() {
        return new TimeInterval(startsAt, endsAt);
    }

    public List<AppointmentLine> getLines() {
        return List.copyOf(lines);
    }

    public Optional<UUID> customerId() {
        return Optional.ofNullable(customerId);
    }

    public Optional<Instant> holdExpiresAt() {
        return Optional.ofNullable(holdExpiresAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getBarberId() {
        return barberId;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public AppointmentSource getSource() {
        return source;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Money getTotalPrice() {
        return totalPrice;
    }

    private void requireCustomerCanChange(CancellationPolicy policy, Instant now) {
        requireStatus(CHANGEABLE);
        if (!policy.allowsChangeAt(startsAt, now)) {
            throw new ChangeDeadlinePassedException(policy.noticeHours());
        }
    }

    private void requireStarted(Instant now) {
        if (now.isBefore(startsAt)) {
            throw AppointmentStatusException.notStartedYet();
        }
    }

    private void requireStatus(Set<AppointmentStatus> allowed) {
        if (!allowed.contains(status)) {
            throw AppointmentStatusException.notAllowedFrom(status);
        }
    }

    private void changeStatus(AppointmentStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Appointment appointment && id != null && id.equals(appointment.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
