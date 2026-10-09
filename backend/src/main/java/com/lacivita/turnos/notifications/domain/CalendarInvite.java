package com.lacivita.turnos.notifications.domain;

import com.lacivita.turnos.booking.AppointmentDetails;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Archivo {@code .ics} (iCalendar, RFC 5545) para que el cliente agregue el turno a su calendario.
 *
 * <p>Todos los archivos del mismo turno tienen el mismo identificador: si el turno se mueve o se cancela, el
 * calendario reemplaza el evento en lugar de agregar otro. La revisión del turno indica cuál es el más nuevo.
 */
// DECISIÓN: METHOD:PUBLISH en lugar de REQUEST. REQUEST pide organizador e invitados y los calendarios lo
// muestran como una invitación a responder; el turno ya está reservado, solo hay que agendarlo.
public final class CalendarInvite {

    public static final String FILE_NAME = "turno.ics";
    public static final String CONTENT_TYPE = "text/calendar; charset=UTF-8; method=PUBLISH";

    private static final DateTimeFormatter UTC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final int MAX_LINE_BYTES = 75;
    private static final String CRLF = "\r\n";

    private CalendarInvite() {}

    public static OutgoingMessage.Attachment of(AppointmentDetails appointment, Instant now) {
        String services = String.join(" + ", appointment.services());
        var lines = new StringBuilder();
        line(lines, "BEGIN:VCALENDAR");
        line(lines, "VERSION:2.0");
        line(lines, "PRODID:-//Laciturnos//Turnos//ES");
        line(lines, "CALSCALE:GREGORIAN");
        line(lines, "METHOD:PUBLISH");
        line(lines, "BEGIN:VEVENT");
        line(lines, "UID:" + appointment.id() + "@laciturnos");
        line(lines, "DTSTAMP:" + UTC.format(now.truncatedTo(ChronoUnit.SECONDS)));
        line(lines, "SEQUENCE:" + appointment.revision());
        line(lines, "DTSTART:" + UTC.format(appointment.startsAt()));
        line(lines, "DTEND:" + UTC.format(appointment.endsAt()));
        line(lines, "SUMMARY:" + escape(services + " en " + appointment.businessName()));
        line(lines, "LOCATION:" + escape(appointment.branchName() + ", " + appointment.branchAddress()));
        line(lines, "DESCRIPTION:" + escape("Con " + appointment.barberName()));
        line(lines, "STATUS:" + status(appointment));
        line(lines, "END:VEVENT");
        line(lines, "END:VCALENDAR");
        return new OutgoingMessage.Attachment(
                FILE_NAME, CONTENT_TYPE, lines.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String status(AppointmentDetails appointment) {
        if (appointment.isCancelled()) {
            return "CANCELLED";
        }
        return appointment.isPending() ? "TENTATIVE" : "CONFIRMED";
    }

    /** Escapa el texto según la RFC 5545: barra invertida, punto y coma, coma y saltos de línea. */
    static String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    /**
     * Agrega una línea, partida cada 75 bytes como pide la RFC 5545 (las continuaciones empiezan con un
     * espacio). Nunca corta un carácter de varios bytes, como una letra con tilde.
     */
    private static void line(StringBuilder out, String content) {
        int bytes = 0;
        for (int i = 0; i < content.length(); ) {
            int codePoint = content.codePointAt(i);
            int size = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8).length;
            if (bytes + size > MAX_LINE_BYTES) {
                out.append(CRLF).append(' ');
                bytes = 1;
            }
            out.appendCodePoint(codePoint);
            bytes += size;
            i += Character.charCount(codePoint);
        }
        out.append(CRLF);
    }
}
