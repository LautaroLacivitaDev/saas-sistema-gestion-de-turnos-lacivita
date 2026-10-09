package com.lacivita.turnos.booking;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.Money;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lo que otros módulos necesitan de un turno para avisar sobre él, con los nombres ya resueltos.
 *
 * @param customerEmail email del cliente; nulo si solo dejó su teléfono
 * @param status estado del turno, por ejemplo {@code CONFIRMED}
 * @param services nombres de los servicios, en el orden en que se reservaron
 * @param revision cuántas veces cambió el turno (los calendarios lo usan para reemplazar el evento)
 */
public record AppointmentDetails(
        UUID id,
        UUID businessId,
        String businessName,
        UUID branchId,
        String branchName,
        String branchAddress,
        ZoneId timeZone,
        UUID barberId,
        String barberName,
        String customerName,
        Email customerEmail,
        String status,
        Instant startsAt,
        Instant endsAt,
        List<String> services,
        Money totalPrice,
        long revision) {

    private static final Set<String> UPCOMING = Set.of("PENDING", "CONFIRMED");

    public AppointmentDetails {
        services = List.copyOf(services);
    }

    /** Todavía va a ocurrir: está confirmado o a confirmar. */
    public boolean isUpcoming() {
        return UPCOMING.contains(status);
    }

    /** El local lo cargó y espera que el cliente confirme. */
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }
}
