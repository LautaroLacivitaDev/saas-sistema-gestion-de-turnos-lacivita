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

    /**
     * Un cliente en el listado del panel.
     *
     * @param appointments cuántos turnos tuvo (sin contar los horarios que no se confirmaron)
     * @param lastAppointmentAt su turno más reciente, pasado o próximo; nulo si nunca tuvo
     */
    public record CustomerSummaryView(
            UUID id, String name, String email, String phone, long appointments, Instant lastAppointmentAt) {}

    /**
     * Ficha de un cliente: contacto, lo que anotó el equipo y su historial.
     *
     * @param hasAccount {@code true} si reservó con una cuenta de Laciturnos
     * @param history sus últimos turnos, del más nuevo al más viejo
     */
    public record CustomerDetailView(
            UUID id,
            String name,
            String email,
            String phone,
            String notes,
            String preferences,
            boolean hasAccount,
            Instant createdAt,
            List<AppointmentView> history) {}

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
