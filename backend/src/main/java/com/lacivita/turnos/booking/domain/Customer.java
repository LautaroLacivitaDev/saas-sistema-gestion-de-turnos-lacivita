package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.PhoneNumber;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Cliente de un negocio. Cada negocio tiene su propia base de clientes: la misma persona en dos negocios
 * son dos clientes distintos. Puede estar vinculado a una cuenta o ser solo un contacto.
 */
@Entity
@Table(name = "customer")
public class Customer {

    static final int MAX_NAME_LENGTH = 120;

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    /** Cuenta de la persona; nula si reservó como invitada o la cargó el equipo. */
    private UUID userId;

    private String name;

    /** Puede faltar si el equipo cargó al cliente solo con su teléfono. */
    private Email email;

    private PhoneNumber phone;

    private String notes;

    private String preferences;

    private Instant createdAt;

    private Instant updatedAt;

    protected Customer() {
        // Requerido por JPA.
    }

    private Customer(UUID businessId, UUID userId, Contact contact, Instant now) {
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.userId = userId;
        this.createdAt = now;
        apply(contact, now);
    }

    public static Customer of(UUID businessId, Contact contact, Instant now) {
        return new Customer(businessId, null, contact, now);
    }

    public static Customer ofAccount(UUID businessId, UUID userId, Contact contact, Instant now) {
        return new Customer(businessId, Objects.requireNonNull(userId, "userId"), contact, now);
    }

    /** Actualiza el contacto con lo último que informó la persona, sin borrar lo que no mandó. */
    public void refresh(Contact contact, Instant now) {
        this.name = contact.name();
        if (contact.email() != null) {
            this.email = contact.email();
        }
        if (contact.phone() != null) {
            this.phone = contact.phone();
        }
        this.updatedAt = now;
    }

    /**
     * El equipo corrige el contacto (por ejemplo, un teléfono mal cargado). A diferencia de {@link #refresh},
     * reemplaza todo: un dato que falta se borra.
     */
    public void correct(Contact contact, Instant now) {
        apply(contact, now);
    }

    public void annotate(CustomerNotes customerNotes, Instant now) {
        this.notes = customerNotes.notes();
        this.preferences = customerNotes.preferences();
        this.updatedAt = now;
    }

    public CustomerNotes notes() {
        return new CustomerNotes(notes, preferences);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** Vincula al cliente con la cuenta con la que reservó. */
    public void linkAccount(UUID accountId) {
        if (userId == null) {
            this.userId = Objects.requireNonNull(accountId, "accountId");
        }
    }

    public Contact contact() {
        return new Contact(name, email, phone);
    }

    public Optional<UUID> userId() {
        return Optional.ofNullable(userId);
    }

    public UUID getId() {
        return id;
    }

    private void apply(Contact contact, Instant now) {
        this.name = contact.name();
        this.email = contact.email();
        this.phone = contact.phone();
        this.updatedAt = now;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Customer customer && id != null && id.equals(customer.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * Datos de contacto de un cliente. Hace falta al menos un email o un teléfono para poder avisarle.
     *
     * @param email puede faltar si hay teléfono
     * @param phone puede faltar si hay email
     */
    public record Contact(String name, Email email, PhoneNumber phone) {

        public Contact {
            if (name == null || name.isBlank()) {
                throw new InvalidValueException("invalid_customer_name", "Ingresá el nombre.");
            }
            name = name.strip();
            if (name.length() > MAX_NAME_LENGTH) {
                throw new InvalidValueException(
                        "invalid_customer_name", "El nombre puede tener hasta " + MAX_NAME_LENGTH + " caracteres.");
            }
            if (email == null && phone == null) {
                throw new InvalidValueException(
                        "contact_required", "Ingresá un email o un teléfono para poder avisarte.");
            }
        }
    }
}
