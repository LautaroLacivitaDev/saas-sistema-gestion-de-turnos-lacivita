package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.application.BookingViews.CustomerDetailView;
import com.lacivita.turnos.booking.application.BookingViews.CustomerSummaryView;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.AppointmentStatus;
import com.lacivita.turnos.booking.domain.Customer;
import com.lacivita.turnos.booking.domain.CustomerEmailTakenException;
import com.lacivita.turnos.booking.domain.CustomerNotFoundException;
import com.lacivita.turnos.booking.domain.CustomerNotes;
import com.lacivita.turnos.booking.domain.CustomerRepository;
import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.PhoneNumber;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de clientes del negocio: buscar, ver la ficha con el historial, corregir el contacto y anotar.
 *
 * <p>La ven el dueño y los gerentes. Un barbero ve el contacto de los clientes de sus turnos en la agenda.
 */
// DECISIÓN: la base de clientes es del negocio entero, no de una sucursal: un gerente ve a todos los
// clientes (un cliente puede atenderse en varias sucursales). Los barberos no tienen acceso a la base,
// que es la regla por defecto de la especificación (solo ven el contacto de sus propios clientes).
@Service
public class CustomerBook {

    /** Turnos que se muestran en la ficha. */
    static final int HISTORY_SIZE = 50;

    private static final String EMAIL_TAKEN = "customer_email_uk";

    private final CustomerRepository customers;
    private final AppointmentRepository appointments;
    private final AppointmentViewer viewer;
    private final Clock clock;

    CustomerBook(
            CustomerRepository customers, AppointmentRepository appointments, AppointmentViewer viewer, Clock clock) {
        this.customers = customers;
        this.appointments = appointments;
        this.viewer = viewer;
        this.clock = clock;
    }

    /** Por nombre, email o teléfono, sin tildes y por partes. Sin texto, todos, por nombre. */
    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public Page<CustomerSummaryView> search(@BusinessId UUID businessId, String text, Pageable pageable) {
        String query = text == null ? "" : text.strip();
        return customers
                .search(escapeLike(query), query.replaceAll("\\D", ""), pageable)
                .map(row -> new CustomerSummaryView(
                        row.getId(),
                        row.getName(),
                        row.getEmail(),
                        row.getPhone(),
                        row.getAppointments(),
                        row.getLastAppointmentAt()));
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public CustomerDetailView detail(@BusinessId UUID businessId, UUID customerId) {
        return detailOf(businessId, require(customerId));
    }

    /** Corrige el contacto y las notas. Un email que ya tiene otro cliente del negocio se rechaza. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public CustomerDetailView update(
            @BusinessId UUID businessId, UUID customerId, Customer.Contact contact, CustomerNotes notes) {
        var customer = require(customerId);
        var now = clock.instant();
        customer.correct(contact, now);
        customer.annotate(notes, now);
        try {
            customers.flush();
        } catch (DataIntegrityViolationException ex) {
            String message = ex.getMostSpecificCause().getMessage();
            if (message != null && message.contains(EMAIL_TAKEN)) {
                throw new CustomerEmailTakenException();
            }
            throw ex;
        }
        return detailOf(businessId, customer);
    }

    private Customer require(UUID customerId) {
        return customers.findById(customerId).orElseThrow(CustomerNotFoundException::new);
    }

    private CustomerDetailView detailOf(UUID businessId, Customer customer) {
        var history = appointments.findHistoryOf(customer.getId(), AppointmentStatus.IN_AGENDA, Limit.of(HISTORY_SIZE));
        var contact = customer.contact();
        var notes = customer.notes();
        return new CustomerDetailView(
                customer.getId(),
                contact.name(),
                Optional.ofNullable(contact.email()).map(Email::value).orElse(null),
                Optional.ofNullable(contact.phone()).map(PhoneNumber::value).orElse(null),
                notes.notes(),
                notes.preferences(),
                customer.userId().isPresent(),
                customer.getCreatedAt(),
                viewer.views(businessId, history, own -> true));
    }

    /** Lo buscado se usa dentro de un LIKE: sus comodines se buscan como texto. */
    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
