package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.domain.Customer;
import com.lacivita.turnos.booking.domain.CustomerRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Encuentra al cliente de un negocio o lo crea. Una persona que vuelve a reservar es el mismo cliente:
 * se la reconoce por su cuenta, su email o, si el equipo la cargó solo con teléfono, su teléfono.
 */
@Component
class Customers {

    private final CustomerRepository customers;

    Customers(CustomerRepository customers) {
        this.customers = customers;
    }

    /** Cliente con cuenta (reservó con sesión iniciada o confirmó con el código de su email). */
    Customer forAccount(UUID businessId, UUID userId, Customer.Contact contact, Instant now) {
        var existing = customers.findByUserId(userId).or(() -> byContact(contact));
        if (existing.isPresent()) {
            existing.get().linkAccount(userId);
            existing.get().refresh(contact, now);
            return existing.get();
        }
        return customers.save(Customer.ofAccount(businessId, userId, contact, now));
    }

    /** Cliente sin cuenta (invitado o cargado por el equipo). */
    Customer forContact(UUID businessId, Customer.Contact contact, Instant now) {
        var existing = byContact(contact);
        if (existing.isPresent()) {
            existing.get().refresh(contact, now);
            return existing.get();
        }
        return customers.save(Customer.of(businessId, contact, now));
    }

    private Optional<Customer> byContact(Customer.Contact contact) {
        if (contact.email() != null) {
            return customers.findByEmail(contact.email());
        }
        return customers.findFirstByPhoneOrderByCreatedAtAsc(contact.phone());
    }
}
