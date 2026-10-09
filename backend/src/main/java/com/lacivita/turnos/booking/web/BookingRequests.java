package com.lacivita.turnos.booking.web;

import com.lacivita.turnos.booking.domain.AppointmentStatus;
import com.lacivita.turnos.booking.domain.CancellationPolicy;
import com.lacivita.turnos.booking.domain.Customer;
import com.lacivita.turnos.catalog.BookableItem;
import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.PhoneNumber;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** Cuerpos de las solicitudes de reservas y su traducción a objetos del dominio. */
final class BookingRequests {

    private BookingRequests() {}

    /** Un servicio o un combo, nunca los dos. */
    static BookableItem item(UUID serviceId, UUID comboId) {
        if ((serviceId == null) == (comboId == null)) {
            throw new InvalidValueException("invalid_item", "Elegí un servicio o un combo.");
        }
        return serviceId != null ? new BookableItem.ServiceItem(serviceId) : new BookableItem.ComboItem(comboId);
    }

    /** @param barberId vacío para "cualquiera disponible" */
    record HoldData(
            @NotNull(message = "Elegí la sucursal.") UUID branchId,
            UUID serviceId,
            UUID comboId,
            UUID barberId,
            @NotNull(message = "Elegí el horario.") Instant startsAt) {

        BookableItem item() {
            return BookingRequests.item(serviceId, comboId);
        }
    }

    /** @param humanToken lo que devuelve el widget de Cloudflare Turnstile */
    record GuestData(
            @NotBlank(message = "Ingresá tu nombre.") @Size(max = 120)
            String name,

            @NotBlank(message = "Ingresá tu email: te mandamos un código.") @Size(max = 254)
            String email,

            @Size(max = 30) String phone,
            String humanToken) {

        Customer.Contact contact() {
            return new Customer.Contact(name, new Email(email), optionalPhone(phone));
        }
    }

    /** @param code el código del email; vacío si se confirma con sesión y email verificado */
    record ConfirmData(@Size(max = 10) String code) {}

    record TokenData(@NotBlank String token) {}

    record CustomerRescheduleData(
            @NotBlank String token,
            @NotNull(message = "Elegí el horario.") Instant startsAt) {}

    record CustomerData(
            @NotBlank(message = "Ingresá el nombre del cliente.") @Size(max = 120)
            String name,

            @Size(max = 254) String email,
            @Size(max = 30) String phone) {

        Customer.Contact contact() {
            return new Customer.Contact(
                    name, email == null || email.isBlank() ? null : new Email(email), optionalPhone(phone));
        }
    }

    /**
     * Turno que carga el equipo.
     *
     * @param customerId un cliente ya cargado; si falta, se usan los datos de {@code customer}
     * @param confirmed {@code false} lo deja "a confirmar" con el cliente; por defecto, confirmado
     */
    record CounterData(
            @NotNull(message = "Elegí la sucursal.") UUID branchId,
            @NotNull(message = "Elegí el profesional.") UUID barberId,
            UUID serviceId,
            UUID comboId,
            @NotNull(message = "Elegí el horario.") Instant startsAt,
            UUID customerId,
            @Valid CustomerData customer,
            Boolean confirmed) {

        BookableItem item() {
            return BookingRequests.item(serviceId, comboId);
        }

        Customer.Contact contact() {
            return customer == null ? null : customer.contact();
        }

        boolean isConfirmed() {
            return confirmed == null || confirmed;
        }
    }

    record StatusData(@NotNull(message = "Elegí el estado.") AppointmentStatus status) {}

    /** @param barberId otro profesional de la sucursal; vacío para seguir con el mismo */
    record StaffRescheduleData(
            @NotNull(message = "Elegí el horario.") Instant startsAt, UUID barberId) {}

    record SettingsData(@NotNull Integer cancellationNoticeHours) {

        CancellationPolicy policy() {
            return new CancellationPolicy(cancellationNoticeHours);
        }
    }

    private static PhoneNumber optionalPhone(String phone) {
        return phone == null || phone.isBlank() ? null : new PhoneNumber(phone);
    }
}
