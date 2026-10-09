package com.lacivita.turnos.booking.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Modelos de lectura de las reservas. Nunca exponen entidades. */
public final class BookingViews {

    private BookingViews() {}

    /** Un servicio del turno con el precio y la duración copiados al reservar. */
    public record LineView(UUID serviceId, String serviceName, BigDecimal price, int durationMinutes) {}

    public record CustomerView(UUID id, String name, String email, String phone) {}

    /** Horario reservado unos minutos mientras el cliente completa sus datos. */
    public record HoldView(
            UUID holdId,
            Instant expiresAt,
            UUID branchId,
            UUID barberId,
            String barberName,
            Instant startsAt,
            Instant endsAt,
            BigDecimal totalPrice,
            List<LineView> lines) {}

    /**
     * Un turno.
     *
     * @param timeZone zona de la sucursal, para mostrar la hora local
     * @param customer datos del cliente; nulo si quien consulta no los puede ver
     */
    public record AppointmentView(
            UUID id,
            UUID branchId,
            String branchName,
            String timeZone,
            UUID barberId,
            String barberName,
            String status,
            String source,
            Instant startsAt,
            Instant endsAt,
            BigDecimal totalPrice,
            List<LineView> lines,
            UUID comboId,
            CustomerView customer) {}

    /**
     * Un turno en el historial de una cuenta, con el negocio al que pertenece (para volver a reservar).
     *
     * @param slug link actual del negocio
     */
    public record AccountAppointmentView(String businessName, String slug, AppointmentView appointment) {}

    /**
     * Turno visto desde el link del cliente.
     *
     * @param slug link actual del negocio, para buscar horarios o reservar otro turno
     * @param changeableUntil hasta cuándo puede cancelarlo o reprogramarlo por su cuenta
     */
    public record ManagedAppointmentView(
            String businessName, String slug, AppointmentView appointment, Instant changeableUntil) {}

    public record SettingsView(int cancellationNoticeHours) {}
}
