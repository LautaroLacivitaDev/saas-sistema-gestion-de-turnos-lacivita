package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.application.BookingViews.AppointmentView;
import com.lacivita.turnos.booking.application.BookingViews.HoldView;
import com.lacivita.turnos.booking.domain.Appointment;
import com.lacivita.turnos.booking.domain.AppointmentLine;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.AppointmentStatus;
import com.lacivita.turnos.booking.domain.Customer;
import com.lacivita.turnos.booking.domain.GuestCheck;
import com.lacivita.turnos.booking.domain.GuestCheckRepository;
import com.lacivita.turnos.booking.domain.GuestDetailsMissingException;
import com.lacivita.turnos.booking.domain.HumanCheck;
import com.lacivita.turnos.booking.domain.HumanCheckFailedException;
import com.lacivita.turnos.booking.domain.InvalidCodeException;
import com.lacivita.turnos.booking.domain.ManageToken;
import com.lacivita.turnos.booking.domain.SlotNotAvailableException;
import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.business.BusinessSummary;
import com.lacivita.turnos.catalog.BookableItem;
import com.lacivita.turnos.catalog.Quote;
import com.lacivita.turnos.catalog.ServiceQuotes;
import com.lacivita.turnos.schedule.FreeBarbers;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.AccountDirectory;
import com.lacivita.turnos.users.TeamDirectory;
import com.lacivita.turnos.users.TeamMember;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reserva online, en tres pasos y sin exigir cuenta:
 *
 * <ol>
 *   <li>El cliente elige un horario: queda reservado unos minutos ({@code HOLD}). Con "cualquiera
 *       disponible", se asigna al profesional libre con menos turnos ese día.
 *   <li>Si no tiene sesión con el email verificado, deja sus datos, pasa la verificación anti-bots y recibe
 *       un código por email.
 *   <li>Confirma: el turno copia el precio y la duración de cada servicio y le llega el link para
 *       gestionarlo.
 * </ol>
 */
// DECISIÓN: "cualquiera disponible" asigna al profesional con menos turnos ese día. La especificación pide
// que el criterio sea configurable (rotación, preferido del cliente); los otros criterios llegan con el
// historial de clientes.
@Service
public class OnlineBooking {

    private final AppointmentRepository appointments;
    private final GuestCheckRepository guestChecks;
    private final AppointmentSlots slots;
    private final Customers customers;
    private final FreeBarbers freeBarbers;
    private final ServiceQuotes quotes;
    private final TeamDirectory team;
    private final AccountDirectory accounts;
    private final BusinessDirectory businesses;
    private final BookingActors actors;
    private final HumanCheck humanCheck;
    private final BookingEmails emails;
    private final AppointmentViewer viewer;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    OnlineBooking(
            AppointmentRepository appointments,
            GuestCheckRepository guestChecks,
            AppointmentSlots slots,
            Customers customers,
            FreeBarbers freeBarbers,
            ServiceQuotes quotes,
            TeamDirectory team,
            AccountDirectory accounts,
            BusinessDirectory businesses,
            BookingActors actors,
            HumanCheck humanCheck,
            BookingEmails emails,
            AppointmentViewer viewer,
            ApplicationEventPublisher events,
            Clock clock) {
        this.appointments = appointments;
        this.guestChecks = guestChecks;
        this.slots = slots;
        this.customers = customers;
        this.freeBarbers = freeBarbers;
        this.quotes = quotes;
        this.team = team;
        this.accounts = accounts;
        this.businesses = businesses;
        this.actors = actors;
        this.humanCheck = humanCheck;
        this.emails = emails;
        this.viewer = viewer;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Paso 1: reserva el horario unos minutos.
     *
     * @param barberId un profesional en particular; {@code null} para "cualquiera disponible"
     */
    @BusinessScoped
    @Transactional
    public HoldView hold(@BusinessId UUID businessId, UUID branchId, BookableItem item, UUID barberId, Instant start) {
        var branch = actors.branchOf(businessId, branchId);
        List<UUID> free = freeBarbers.at(businessId, branchId, item, barberId, start, null);
        if (free.isEmpty()) {
            throw new SlotNotAvailableException();
        }
        var now = clock.instant();
        var chosen = barberId != null
                ? barberId
                : leastBusy(free, LocalDate.ofInstant(start, branch.timeZone()), branch.timeZone(), now);
        var quote = quotes.quote(businessId, chosen, item).orElseThrow(SlotNotAvailableException::new);
        var hold = slots.take(Appointment.hold(businessId, branchId, chosen, item, lines(quote), start, now), now);
        var barberName = team.member(businessId, chosen).map(TeamMember::name).orElse(null);
        var interval = hold.interval();
        return new HoldView(
                hold.getId(),
                hold.holdExpiresAt().orElseThrow(),
                branchId,
                chosen,
                barberName,
                interval.start(),
                interval.end(),
                hold.getTotalPrice().amount(),
                AppointmentViewer.lines(hold));
    }

    /** Paso 2 (invitados): guarda sus datos y le manda un código por email. Pedir otro reemplaza el anterior. */
    @BusinessScoped
    @Transactional
    public void sendGuestCode(
            @BusinessId UUID businessId, UUID holdId, Customer.Contact contact, String humanToken, String remoteIp) {
        if (!humanCheck.isHuman(humanToken, remoteIp)) {
            throw new HumanCheckFailedException();
        }
        var now = clock.instant();
        var hold = appointments.require(holdId);
        hold.requireLiveHold(now);
        var check = guestChecks.findById(holdId).orElseGet(() -> GuestCheck.forHold(hold));
        String code = check.issueCode(contact, now);
        guestChecks.save(check);
        emails.sendCode(contact.email(), contact.name(), businessName(businessId), code);
    }

    /**
     * Paso 3: confirma el turno. Con sesión y email verificado alcanza; si no, hace falta el código del
     * email. Un código equivocado cuenta como intento aunque la solicitud falle.
     *
     * @param user persona con sesión; {@code null} si reserva como invitada
     */
    @BusinessScoped
    @Transactional(noRollbackFor = InvalidCodeException.class)
    public AppointmentView confirm(@BusinessId UUID businessId, UUID holdId, String code, AuthenticatedUser user) {
        var now = clock.instant();
        var hold = appointments.require(holdId);
        hold.requireLiveHold(now);

        Customer customer;
        if (user != null && accounts.hasVerifiedEmail(user.id())) {
            var contact = guestChecks
                    .findById(holdId)
                    .map(GuestCheck::contact)
                    .orElseGet(() -> new Customer.Contact(user.name(), user.email(), null));
            customer = customers.forAccount(businessId, user.id(), contact, now);
        } else {
            var check = guestChecks.findById(holdId).orElseThrow(GuestDetailsMissingException::new);
            if (!check.matches(code, now)) {
                throw new InvalidCodeException();
            }
            customer = user != null
                    ? customers.forAccount(businessId, user.id(), check.contact(), now)
                    : customers.forContact(businessId, check.contact(), now);
        }
        guestChecks.findById(holdId).ifPresent(guestChecks::delete);

        var token = ManageToken.generate();
        hold.confirmHold(customer.getId(), token, now);
        events.publishEvent(new BookingEvents.AppointmentBooked(
                businessId,
                hold.getId(),
                hold.getBarberId(),
                hold.getStartsAt(),
                hold.getSource().name(),
                hold.getStatus().name()));

        var view = viewer.view(businessId, hold);
        var contact = customer.contact();
        if (contact.email() != null) {
            emails.sendConfirmation(
                    contact.email(),
                    contact.name(),
                    businessName(businessId),
                    actors.branchOf(businessId, hold.getBranchId()),
                    view.barberName(),
                    hold,
                    token);
        }
        return view;
    }

    /** El profesional libre con menos turnos ese día (a igualdad, siempre el mismo). */
    private UUID leastBusy(List<UUID> free, LocalDate day, ZoneId zone, Instant now) {
        var from = day.atStartOfDay(zone).toInstant();
        var to = day.plusDays(1).atStartOfDay(zone).toInstant();
        return free.stream()
                .min(Comparator.comparingInt((UUID barber) -> appointments
                                .findOccupying(barber, AppointmentStatus.OCCUPYING, from, to, now)
                                .size())
                        .thenComparing(UUID::toString))
                .orElseThrow();
    }

    private String businessName(UUID businessId) {
        return businesses.find(businessId).map(BusinessSummary::name).orElse("");
    }

    static List<AppointmentLine> lines(Quote quote) {
        return quote.lines().stream()
                .map(line -> new AppointmentLine(line.serviceId(), line.serviceName(), line.price(), (int)
                        line.duration().toMinutes()))
                .toList();
    }
}
