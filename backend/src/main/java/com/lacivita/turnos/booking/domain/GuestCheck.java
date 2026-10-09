package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.PhoneNumber;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Verificación de quien reserva como invitado: sus datos y el código de 6 dígitos que le llega por email.
 * Confirmar el turno con ese código prueba que el email es suyo.
 *
 * <p>El código vence a los 10 minutos y admite 5 intentos. Pedir otro reemplaza el anterior.
 */
@Entity
@Table(name = "guest_check")
public class GuestCheck {

    static final Duration VALIDITY = Duration.ofMinutes(10);
    static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** El mismo id del turno en {@code HOLD}. */
    @Id
    private UUID appointmentId;

    @TenantId
    private UUID businessId;

    private String name;

    private Email email;

    private PhoneNumber phone;

    private String codeHash;

    private Instant expiresAt;

    private int attempts;

    protected GuestCheck() {
        // Requerido por JPA.
    }

    private GuestCheck(UUID businessId, UUID appointmentId) {
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId");
    }

    public static GuestCheck forHold(Appointment hold) {
        return new GuestCheck(hold.getBusinessId(), hold.getId());
    }

    /**
     * Guarda los datos del invitado y genera un código nuevo.
     *
     * @return el código en claro, para enviarlo por email; en la base queda solo su hash
     */
    public String issueCode(Customer.Contact contact, Instant now) {
        Objects.requireNonNull(contact.email(), "Para reservar como invitado hace falta un email");
        this.name = contact.name();
        this.email = contact.email();
        this.phone = contact.phone();
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        this.codeHash = hash(code);
        this.expiresAt = now.plus(VALIDITY);
        this.attempts = 0;
        return code;
    }

    /**
     * Compara el código. Un error cuenta como intento; al quinto, el código ya no sirve aunque sea el
     * correcto.
     *
     * @return {@code true} si el código es correcto y sigue vigente
     */
    public boolean matches(String code, Instant now) {
        if (codeHash == null || attempts >= MAX_ATTEMPTS || !now.isBefore(expiresAt)) {
            return false;
        }
        boolean matches = code != null
                && MessageDigest.isEqual(
                        hash(code.strip()).getBytes(StandardCharsets.UTF_8), codeHash.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            attempts++;
        }
        return matches;
    }

    public boolean hasAttemptsLeft() {
        return attempts < MAX_ATTEMPTS;
    }

    public Customer.Contact contact() {
        return new Customer.Contact(name, email, phone);
    }

    public Email getEmail() {
        return email;
    }

    /** El código se mezcla con el id del turno: el mismo código en dos turnos no tiene el mismo hash. */
    private String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((appointmentId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof GuestCheck check
                        && appointmentId != null
                        && appointmentId.equals(check.appointmentId));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
