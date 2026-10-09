package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.application.BookingViews.AppointmentView;
import com.lacivita.turnos.booking.application.BookingViews.CustomerView;
import com.lacivita.turnos.booking.application.BookingViews.LineView;
import com.lacivita.turnos.booking.domain.Appointment;
import com.lacivita.turnos.booking.domain.AppointmentLine;
import com.lacivita.turnos.booking.domain.Customer;
import com.lacivita.turnos.booking.domain.CustomerRepository;
import com.lacivita.turnos.business.BranchSummary;
import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.PhoneNumber;
import com.lacivita.turnos.users.TeamDirectory;
import com.lacivita.turnos.users.TeamMember;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Arma las vistas de los turnos con los nombres de la sucursal, del profesional y del cliente, con pocas
 * consultas para cualquier cantidad de turnos (sin N+1).
 */
@Component
class AppointmentViewer {

    private final BusinessDirectory businesses;
    private final TeamDirectory team;
    private final CustomerRepository customers;

    AppointmentViewer(BusinessDirectory businesses, TeamDirectory team, CustomerRepository customers) {
        this.businesses = businesses;
        this.team = team;
        this.customers = customers;
    }

    /** @param showContact qué turnos muestran los datos del cliente */
    List<AppointmentView> views(
            UUID businessId, Collection<Appointment> appointments, Predicate<Appointment> showContact) {
        Map<UUID, String> barberNames = team
                .members(
                        businessId,
                        appointments.stream().map(Appointment::getBarberId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(TeamMember::userId, TeamMember::name));
        Map<UUID, Customer> customersById = customers
                .findAllByIdIn(appointments.stream()
                        .flatMap(appointment -> appointment.customerId().stream())
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<UUID, Optional<BranchSummary>> branches = new HashMap<>();
        return appointments.stream()
                .map(appointment -> view(
                        appointment,
                        branches.computeIfAbsent(appointment.getBranchId(), id -> businesses.branch(businessId, id)),
                        barberNames.get(appointment.getBarberId()),
                        showContact.test(appointment)
                                ? appointment
                                        .customerId()
                                        .map(customersById::get)
                                        .orElse(null)
                                : null))
                .toList();
    }

    AppointmentView view(UUID businessId, Appointment appointment) {
        return views(businessId, List.of(appointment), any -> true).getFirst();
    }

    static List<LineView> lines(Appointment appointment) {
        return appointment.getLines().stream().map(AppointmentViewer::line).toList();
    }

    private static AppointmentView view(
            Appointment appointment, Optional<BranchSummary> branch, String barberName, Customer customer) {
        var interval = appointment.interval();
        return new AppointmentView(
                appointment.getId(),
                appointment.getBranchId(),
                branch.map(BranchSummary::name).orElse(null),
                branch.map(b -> b.timeZone().getId()).orElse(null),
                appointment.getBarberId(),
                barberName,
                appointment.getStatus().name(),
                appointment.getSource().name(),
                interval.start(),
                interval.end(),
                appointment.getTotalPrice().amount(),
                lines(appointment),
                appointment.comboId().orElse(null),
                customer == null ? null : customer(customer));
    }

    private static LineView line(AppointmentLine line) {
        return new LineView(line.serviceId(), line.serviceName(), line.price().amount(), line.durationMinutes());
    }

    private static CustomerView customer(Customer customer) {
        var contact = customer.contact();
        return new CustomerView(
                customer.getId(),
                contact.name(),
                Optional.ofNullable(contact.email()).map(Email::value).orElse(null),
                Optional.ofNullable(contact.phone()).map(PhoneNumber::value).orElse(null));
    }
}
