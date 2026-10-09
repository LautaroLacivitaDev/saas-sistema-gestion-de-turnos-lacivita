package com.lacivita.turnos.notifications.application;

import com.lacivita.turnos.booking.AppointmentDetails;
import com.lacivita.turnos.notifications.domain.EmailContent.Detail;
import com.lacivita.turnos.notifications.domain.TemplateVariable;
import com.lacivita.turnos.shared.domain.Money;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Textos fijos de las notificaciones (los del equipo y los datos del turno) y el formato de fechas e
 * importes, todo en español rioplatense y en un solo lugar.
 */
final class NotificationTexts {

    private static final Locale ES_AR = Locale.of("es", "AR");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'a las' HH:mm", ES_AR);
    private static final DateTimeFormatter SHORT_WHEN = DateTimeFormatter.ofPattern("EEE d/M HH:mm", ES_AR);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", ES_AR);

    // Botones del email al cliente.
    static final String CONFIRM = "Confirmar que voy";
    static final String RESCHEDULE = "Cambiar día u horario";
    static final String CANCEL = "Cancelar turno";

    private NotificationTexts() {}

    /** Por ejemplo "viernes 9 de octubre a las 10:00", en la hora de la sucursal. */
    static String when(Instant instant, ZoneId zone) {
        return WHEN.format(instant.atZone(zone));
    }

    /** Por ejemplo "vie 9/10 10:00". */
    static String shortWhen(Instant instant, ZoneId zone) {
        return SHORT_WHEN.format(instant.atZone(zone)).replace(".", "");
    }

    static String time(Instant instant, ZoneId zone) {
        return TIME.format(instant.atZone(zone));
    }

    /** Por ejemplo "$ 9.000" o "$ 9.500,50". */
    static String money(Money money) {
        BigDecimal amount = money.amount();
        String pattern = amount.stripTrailingZeros().scale() <= 0 ? "#,##0" : "#,##0.00";
        return "$ " + new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(ES_AR)).format(amount);
    }

    static String services(AppointmentDetails appointment) {
        return String.join(" + ", appointment.services());
    }

    /** Los valores de las variables que el negocio usa en sus textos. */
    static Map<TemplateVariable, String> variables(AppointmentDetails appointment) {
        var values = new EnumMap<TemplateVariable, String>(TemplateVariable.class);
        values.put(TemplateVariable.NOMBRE, appointment.customerName());
        values.put(TemplateVariable.SERVICIO, services(appointment));
        values.put(TemplateVariable.BARBERO, appointment.barberName());
        values.put(TemplateVariable.SUCURSAL, appointment.branchName());
        values.put(TemplateVariable.DIRECCION, appointment.branchAddress());
        values.put(TemplateVariable.HORA, when(appointment.startsAt(), appointment.timeZone()));
        values.put(TemplateVariable.NEGOCIO, appointment.businessName());
        return values;
    }

    /** El resumen del turno que muestran los emails al cliente. */
    static List<Detail> customerDetails(AppointmentDetails appointment) {
        return List.of(
                new Detail("Servicio", services(appointment)),
                new Detail("Profesional", appointment.barberName()),
                new Detail("Día y hora", when(appointment.startsAt(), appointment.timeZone())),
                new Detail("Sucursal", appointment.branchName()),
                new Detail("Dirección", appointment.branchAddress()),
                new Detail("Total", money(appointment.totalPrice())));
    }

    /** El resumen del turno que muestran los emails al equipo. */
    static List<Detail> teamDetails(AppointmentDetails appointment) {
        return List.of(
                new Detail("Cliente", appointment.customerName()),
                new Detail("Servicio", services(appointment)),
                new Detail("Profesional", appointment.barberName()),
                new Detail("Día y hora", when(appointment.startsAt(), appointment.timeZone())),
                new Detail("Sucursal", appointment.branchName()));
    }

    static String customerFooter(String businessName) {
        return "Este email lo envía Laciturnos en nombre de " + businessName + ".";
    }

    static String teamFooter(String businessName) {
        return "Aviso de Laciturnos para el equipo de " + businessName + ".";
    }

    // Avisos en la app.

    static String bookedNotice(AppointmentDetails appointment) {
        return "Nuevo turno: " + services(appointment) + " con " + appointment.customerName() + ", "
                + shortWhen(appointment.startsAt(), appointment.timeZone()) + " en " + appointment.branchName() + ".";
    }

    static String movedNotice(AppointmentDetails appointment) {
        return "Turno movido: " + services(appointment) + " con " + appointment.customerName() + ", ahora el "
                + shortWhen(appointment.startsAt(), appointment.timeZone()) + " en " + appointment.branchName() + ".";
    }

    static String reassignedNotice(AppointmentDetails appointment, Instant startBefore) {
        return "Turno reasignado: " + services(appointment) + " con " + appointment.customerName() + " del "
                + shortWhen(startBefore, appointment.timeZone()) + " pasó a " + appointment.barberName() + ".";
    }

    static String cancelledNotice(AppointmentDetails appointment) {
        return "Turno cancelado: " + services(appointment) + " con " + appointment.customerName() + ", "
                + shortWhen(appointment.startsAt(), appointment.timeZone()) + " en " + appointment.branchName() + ".";
    }

    // Emails al equipo.

    static String bookedSubject(AppointmentDetails appointment) {
        return "Nuevo turno: " + shortWhen(appointment.startsAt(), appointment.timeZone());
    }

    static String bookedMessage(AppointmentDetails appointment) {
        return "Tenés un turno nuevo en " + appointment.branchName() + ".";
    }

    static String movedSubject(AppointmentDetails appointment) {
        return "Se movió un turno: " + shortWhen(appointment.startsAt(), appointment.timeZone());
    }

    static String movedMessage(AppointmentDetails appointment) {
        return "Un turno tuyo cambió. Ahora es el " + when(appointment.startsAt(), appointment.timeZone()) + ".";
    }

    static String reassignedSubject() {
        return "Se reasignó un turno";
    }

    static String reassignedMessage(AppointmentDetails appointment) {
        return "Un turno que tenías pasó a " + appointment.barberName() + ".";
    }

    static String cancelledSubject(AppointmentDetails appointment) {
        return "Se canceló un turno: " + shortWhen(appointment.startsAt(), appointment.timeZone());
    }

    static String cancelledMessage(AppointmentDetails appointment) {
        return "Se canceló el turno de " + appointment.customerName() + " del "
                + when(appointment.startsAt(), appointment.timeZone()) + ".";
    }

    static String agendaSubject(String businessName) {
        return "Tu agenda de hoy en " + businessName;
    }

    static String agendaMessage(int count) {
        return count == 1 ? "Hoy tenés 1 turno:" : "Hoy tenés " + count + " turnos:";
    }

    static String agendaItem(AppointmentDetails appointment) {
        String item = time(appointment.startsAt(), appointment.timeZone()) + " · " + services(appointment) + " · "
                + appointment.customerName() + " · " + appointment.branchName();
        return appointment.isPending() ? item + " (a confirmar)" : item;
    }
}
