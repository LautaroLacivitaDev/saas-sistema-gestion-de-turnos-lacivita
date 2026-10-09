package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.AppointmentDetails;
import com.lacivita.turnos.booking.AppointmentDirectory;
import com.lacivita.turnos.booking.domain.Appointment;
import com.lacivita.turnos.booking.domain.AppointmentLine;
import com.lacivita.turnos.booking.domain.AppointmentLink;
import com.lacivita.turnos.booking.domain.AppointmentLinkRepository;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.AppointmentStatus;
import com.lacivita.turnos.booking.domain.BookingPolicy;
import com.lacivita.turnos.booking.domain.Customer;
import com.lacivita.turnos.booking.domain.CustomerRepository;
import com.lacivita.turnos.booking.domain.ManageToken;
import com.lacivita.turnos.business.BranchSummary;
import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.business.BusinessSummary;
import com.lacivita.turnos.shared.config.AppProperties;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.TeamDirectory;
import com.lacivita.turnos.users.TeamMember;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AppointmentDirectoryService implements AppointmentDirectory {

    /** Página del frontend donde el cliente gestiona su turno. */
    static final String MANAGE_PATH = "/turno?token=";

    private final AppointmentRepository appointments;
    private final AppointmentLinkRepository links;
    private final CustomerRepository customers;
    private final BusinessDirectory businesses;
    private final TeamDirectory team;
    private final AppProperties properties;
    private final Clock clock;

    AppointmentDirectoryService(
            AppointmentRepository appointments,
            AppointmentLinkRepository links,
            CustomerRepository customers,
            BusinessDirectory businesses,
            TeamDirectory team,
            AppProperties properties,
            Clock clock) {
        this.appointments = appointments;
        this.links = links;
        this.customers = customers;
        this.businesses = businesses;
        this.team = team;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @BusinessScoped
    @Transactional(readOnly = true)
    public Optional<AppointmentDetails> details(@BusinessId UUID businessId, UUID appointmentId) {
        return appointments
                .findById(appointmentId)
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.HOLD)
                .map(appointment -> detailsOf(businessId, List.of(appointment)).getFirst());
    }

    @Override
    @BusinessScoped
    @Transactional(readOnly = true)
    public List<AppointmentDetails> upcomingForBarber(
            @BusinessId UUID businessId, UUID barberId, Instant from, Instant to) {
        var upcoming = appointments.findForBarberInPeriod(
                barberId, EnumSet.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED), from, to);
        return upcoming.isEmpty() ? List.of() : detailsOf(businessId, upcoming);
    }

    @Override
    @BusinessScoped
    @Transactional(readOnly = true)
    public boolean isVisibleTo(@BusinessId UUID businessId, AuthenticatedUser actor, UUID appointmentId) {
        var appointment =
                appointments.findById(appointmentId).filter(found -> found.getStatus() != AppointmentStatus.HOLD);
        if (appointment.isEmpty()) {
            return false;
        }
        var member = team.member(businessId, actor.id());
        if (member.isEmpty()) {
            // Un ADMIN que entra como soporte ve lo mismo que el dueño.
            return actor.isAdmin();
        }
        return BookingPolicy.canSee(member.get().membership(), actor.id(), appointment.get());
    }

    @Override
    @BusinessScoped
    @Transactional
    public String newManageLink(@BusinessId UUID businessId, UUID appointmentId) {
        var appointment = appointments.require(appointmentId);
        var token = ManageToken.generate();
        links.save(AppointmentLink.to(appointment, token, clock.instant()));
        return properties.frontendLink(MANAGE_PATH + URLEncoder.encode(token.value(), StandardCharsets.UTF_8));
    }

    /** Arma los detalles con pocas consultas para cualquier cantidad de turnos (sin N+1). */
    private List<AppointmentDetails> detailsOf(UUID businessId, Collection<Appointment> found) {
        String businessName =
                businesses.find(businessId).map(BusinessSummary::name).orElse("");
        Map<UUID, String> barberNames =
                team
                        .members(
                                businessId,
                                found.stream().map(Appointment::getBarberId).collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(TeamMember::userId, TeamMember::name));
        Map<UUID, Customer> customersById = customers
                .findAllByIdIn(found.stream()
                        .flatMap(appointment -> appointment.customerId().stream())
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<UUID, Optional<BranchSummary>> branches = new HashMap<>();
        return found.stream()
                .map(appointment -> details(
                        appointment,
                        businessName,
                        branches.computeIfAbsent(appointment.getBranchId(), id -> businesses.branch(businessId, id)),
                        barberNames.get(appointment.getBarberId()),
                        appointment.customerId().map(customersById::get)))
                .toList();
    }

    private static AppointmentDetails details(
            Appointment appointment,
            String businessName,
            Optional<BranchSummary> branch,
            String barberName,
            Optional<Customer> customer) {
        var interval = appointment.interval();
        var contact = customer.map(Customer::contact);
        return new AppointmentDetails(
                appointment.getId(),
                appointment.getBusinessId(),
                businessName,
                appointment.getBranchId(),
                branch.map(BranchSummary::name).orElse(""),
                branch.map(BranchSummary::address).orElse(""),
                branch.map(BranchSummary::timeZone).orElse(ZoneOffset.UTC),
                appointment.getBarberId(),
                barberName == null ? "" : barberName,
                contact.map(Customer.Contact::name).orElse(""),
                contact.map(Customer.Contact::email).orElse(null),
                appointment.getStatus().name(),
                interval.start(),
                interval.end(),
                appointment.getLines().stream()
                        .map(AppointmentLine::serviceName)
                        .toList(),
                appointment.getTotalPrice(),
                appointment.revision());
    }
}
