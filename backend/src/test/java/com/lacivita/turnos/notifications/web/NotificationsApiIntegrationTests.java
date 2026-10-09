package com.lacivita.turnos.notifications.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.ApiClient.Session;
import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.RecordingMailer;
import com.lacivita.turnos.RecordingNotificationChannel;
import com.lacivita.turnos.TestClock;
import com.lacivita.turnos.notifications.application.NotificationDispatcher;
import com.lacivita.turnos.notifications.domain.OutgoingMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Avisos de punta a punta: bandeja de salida, envío con reintentos, recordatorios, resumen diario, textos
 * del negocio, avisos en la app y registro de envíos.
 *
 * <p>Los turnos se cargan desde el local (no dependen del horario) dentro de tres días, a las 10:00 de
 * Buenos Aires, para que todos los recordatorios queden en el futuro.
 */
@IntegrationTest
class NotificationsApiIntegrationTests {

    static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordingMailer mailer;

    @Autowired
    RecordingNotificationChannel channel;

    @Autowired
    NotificationDispatcher dispatcher;

    @Autowired
    TestClock clock;

    ApiClient api;
    Session owner;
    Session manager;
    Session barber;
    Session other;
    Session barberNorte;
    UUID businessId;
    String businessName;
    UUID centro;
    UUID corte;
    LocalDate day;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc);
        owner = api.registerNewUser("Dueña");
        String slug = "avisos-" + UUID.randomUUID().toString().substring(0, 8);
        businessId = api.createBusiness(owner, slug);
        businessName = ApiClient.read(api.get(owner, "/api/businesses/" + businessId), "$.name");
        centro = api.createBranch(owner, businessId, "Centro");
        var norte = api.createBranch(owner, businessId, "Norte");
        manager = api.joinTeam(owner, businessId, "MANAGER", centro, mailer, "Gerente");
        barber = api.joinTeam(owner, businessId, "BARBER", centro, mailer, "Barbera");
        other = api.joinTeam(owner, businessId, "BARBER", centro, mailer, "Barbero");
        barberNorte = api.joinTeam(owner, businessId, "BARBER", norte, mailer, "Barbero Norte");
        corte = UUID.fromString(ApiClient.read(api.post(owner, business("/services"), """
                        {"name":"Corte","category":"Cortes","baseDurationMinutes":30,"basePrice":8000}"""), "$.id"));
        api.put(barber, business("/barbers/" + barber.userId() + "/services/" + corte), "{}");
        api.put(other, business("/barbers/" + other.userId() + "/services/" + corte), "{}");
        day = LocalDate.now(BUENOS_AIRES).plusDays(3);
    }

    @AfterEach
    void backToTheRealTime() {
        clock.reset();
    }

    @Nested
    class Booked {

        @Test
        void theCustomerGetsAnEmailWithTheCalendarFileAndButtonsToManageTheAppointment() {
            String email = newEmail();
            book(manager, barber, "10:00", email, true);

            dispatcher.dispatchDue();

            var message = last(email);
            assertThat(message.subject()).isEqualTo("Tu turno en " + businessName);
            assertThat(message.text())
                    .contains("Corte con Barbera")
                    .contains("Cambiar día u horario: http://localhost:3000/turno?token=")
                    .contains("&accion=cancelar")
                    .doesNotContain("Confirmar que voy");
            assertThat(message.html()).contains("Cancelar turno").contains("Centro");
            assertThat(message.attachments()).singleElement().satisfies(file -> {
                assertThat(file.fileName()).isEqualTo("turno.ics");
                assertThat(new String(file.content(), StandardCharsets.UTF_8)).contains("STATUS:CONFIRMED");
            });
        }

        @Test
        void theLinkInTheEmailOpensTheAppointment() {
            String email = newEmail();
            book(manager, barber, "10:00", email, true);
            dispatcher.dispatchDue();

            String token = channel.lastManageTokenSentTo(email).orElseThrow();

            assertThat(api.post(null, "/api/public/appointments/lookup", tokenJson(token)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.appointment.customer.email")
                    .isEqualTo(email);
        }

        @Test
        void theBarberGetsANoticeInTheAppAndAnEmail() {
            book(manager, barber, "10:00", newEmail(), true);
            dispatcher.dispatchDue();

            assertThat(unread(barber)).isEqualTo(1);
            assertThat(api.get(barber, business("/notices")))
                    .bodyJson()
                    .extractingPath("$.items[0].message")
                    .asString()
                    .startsWith("Nuevo turno: Corte con Cliente");
            assertThat(last(barber.email()).subject()).startsWith("Nuevo turno: ");
        }

        @Test
        void whoeverMadeTheChangeIsNotNotifiedOfIt() {
            book(barber, barber, "10:00", newEmail(), true);
            dispatcher.dispatchDue();

            assertThat(unread(barber)).isZero();
            assertThat(channel.sentTo(barber.email())).isEmpty();
        }

        @Test
        void anAppointmentToConfirmAsksTheCustomerToConfirmAndTheyCan() {
            String email = newEmail();
            book(manager, barber, "10:00", email, false);
            dispatcher.dispatchDue();

            assertThat(last(email).text()).contains("Confirmar que voy: ");
            String token = channel.lastManageTokenSentTo(email).orElseThrow();
            assertThat(api.post(null, "/api/public/appointments/confirm", tokenJson(token)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.appointment.status")
                    .isEqualTo("CONFIRMED");
        }

        @Test
        void aCustomerWithoutEmailIsSkippedAndTheLogSaysSo() {
            var appointmentId = book(manager, barber, "10:00", null, true);
            dispatcher.dispatchDue();

            assertThat(statuses(appointmentId, "APPOINTMENT_BOOKED", "CUSTOMER"))
                    .containsExactly("SKIPPED");
            assertThat(statuses(appointmentId, "APPOINTMENT_BOOKED", "BARBER")).containsExactly("SENT");
        }
    }

    @Nested
    class Reminders {

        @Test
        void twoRemindersAreScheduledAndGoOutAtTheirTime() {
            String email = newEmail();
            var appointmentId = book(manager, barber, "10:00", email, true);
            dispatcher.dispatchDue();
            assertThat(statuses(appointmentId, "APPOINTMENT_REMINDER", "CUSTOMER"))
                    .containsExactly("PENDING", "PENDING");

            clock.jumpTo(at("10:00").minus(Duration.ofHours(24)).plusSeconds(1));
            dispatcher.dispatchDue();

            assertThat(last(email).subject()).isEqualTo("Recordatorio: tu turno en " + businessName);
            assertThat(statuses(appointmentId, "APPOINTMENT_REMINDER", "CUSTOMER"))
                    .containsExactly("SENT", "PENDING");
        }

        @Test
        void movingTheAppointmentReplacesItsRemindersAndNotifiesEveryone() {
            String email = newEmail();
            var appointmentId = book(manager, barber, "10:00", email, true);

            assertThat(api.put(manager, business("/appointments/" + appointmentId + "/time"), """
                            {"startsAt":"%s","barberId":"%s"}""".formatted(
                                    at("11:00"), other.userId())))
                    .hasStatusOk();
            dispatcher.dispatchDue();

            assertThat(statuses(appointmentId, "APPOINTMENT_REMINDER", "CUSTOMER"))
                    .containsExactly("CANCELLED", "CANCELLED", "PENDING", "PENDING");
            assertThat(last(email).subject()).isEqualTo("Cambió tu turno en " + businessName);
            assertThat(last(email).text()).contains("a las 11:00").contains("con Barbero");
            assertThat(last(other.email()).subject()).startsWith("Se movió un turno");
            assertThat(last(barber.email()).subject()).isEqualTo("Se reasignó un turno");
        }

        @Test
        void cancellingNotifiesTheCustomerAndTheBarberAndStopsTheReminders() {
            String email = newEmail();
            var appointmentId = book(manager, barber, "10:00", email, true);

            api.put(manager, business("/appointments/" + appointmentId + "/status"), """
                    {"status":"CANCELLED"}""");
            dispatcher.dispatchDue();

            assertThat(statuses(appointmentId, "APPOINTMENT_REMINDER", "CUSTOMER"))
                    .containsExactly("CANCELLED", "CANCELLED");
            var cancelled = last(email);
            assertThat(cancelled.subject()).isEqualTo("Se canceló tu turno en " + businessName);
            assertThat(cancelled.text()).doesNotContain("token=");
            assertThat(new String(cancelled.attachments().getFirst().content(), StandardCharsets.UTF_8))
                    .contains("STATUS:CANCELLED");
            assertThat(unread(barber)).isEqualTo(2);
        }

        @Test
        void eachBarberGetsTheDaySummaryInTheMorning() {
            book(manager, barber, "10:00", newEmail(), true);
            book(manager, barber, "11:00", null, false);
            dispatcher.dispatchDue();

            clock.jumpTo(ZonedDateTime.of(day, LocalTime.of(7, 1), BUENOS_AIRES).toInstant());
            dispatcher.dispatchDue();

            var summary = last(barber.email());
            assertThat(summary.subject()).isEqualTo("Tu agenda de hoy en " + businessName);
            assertThat(summary.text())
                    .contains("Hoy tenés 2 turnos:")
                    .contains("- 10:00 · Corte")
                    .contains("- 11:00 · Corte · Cliente de mostrador · Centro (a confirmar)");
        }

        @Test
        void onlyTheOwnerChoosesWhenToRemind() {
            String reminders = """
                    {"reminderHours":[48]}""";

            assertThat(api.put(manager, business("/notification-settings"), reminders))
                    .hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.put(owner, business("/notification-settings"), """
                            {"reminderHours":[0]}""")).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(api.put(owner, business("/notification-settings"), reminders))
                    .hasStatusOk();

            var appointmentId = book(manager, barber, "10:00", newEmail(), true);
            assertThat(statuses(appointmentId, "APPOINTMENT_REMINDER", "CUSTOMER"))
                    .containsExactly("PENDING");
            assertThat(api.get(barber, business("/notification-settings")))
                    .bodyJson()
                    .extractingPath("$.reminderHours[0]")
                    .isEqualTo(48);
        }
    }

    @Nested
    class Delivery {

        @Test
        void aFailedSendIsRetriedAndTheLogShowsTheAttempts() {
            String email = newEmail();
            var appointmentId = book(barber, barber, "10:00", email, true);
            channel.failNextTo(email, 1);

            dispatcher.dispatchDue();
            assertThat(statuses(appointmentId, "APPOINTMENT_BOOKED", "CUSTOMER"))
                    .containsExactly("PENDING");
            assertThat(channel.sentTo(email)).isEmpty();

            clock.advance(Duration.ofMinutes(1).plusSeconds(1));
            dispatcher.dispatchDue();

            assertThat(statuses(appointmentId, "APPOINTMENT_BOOKED", "CUSTOMER"))
                    .containsExactly("SENT");
            assertThat(log(appointmentId))
                    .bodyJson()
                    .extractingPath("$[?(@.type == 'APPOINTMENT_BOOKED')].attempts")
                    .isEqualTo(List.of(2));
            assertThat(channel.sentTo(email)).hasSize(1);
        }

        @Test
        void theLogIsSeenByWhoeverSeesTheAppointment() {
            var appointmentId = book(manager, barber, "10:00", newEmail(), true);
            var stranger = api.registerNewUser("Otra persona");

            assertThat(log(appointmentId, owner)).hasStatusOk();
            assertThat(log(appointmentId, barber)).hasStatusOk();
            assertThat(log(appointmentId, barberNorte)).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(log(appointmentId, stranger)).hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    class Texts {

        @Test
        void theOwnerWritesTheirOwnTextWithTheAppointmentData() {
            assertThat(api.put(owner, business("/message-templates/APPOINTMENT_BOOKED"), """
                            {"subject":"¡Te esperamos, {nombre}!","body":"Tu turno: {hora}.\\n\\nTraé tu cupón."}"""))
                    .hasStatusOk();
            String email = newEmail();

            book(manager, barber, "10:00", email, true);
            dispatcher.dispatchDue();

            var message = last(email);
            assertThat(message.subject()).isEqualTo("¡Te esperamos, Cliente de mostrador!");
            assertThat(message.text())
                    .contains("Tu turno: ")
                    .contains("a las 10:00.")
                    .contains("Traé tu cupón.");
        }

        @Test
        void theTextIsEscapedInTheEmail() {
            api.put(owner, business("/message-templates/APPOINTMENT_BOOKED"), """
                    {"subject":"Hola","body":"<script>alert(1)</script> {nombre}"}""");
            String email = newEmail();

            book(manager, barber, "10:00", email, true);
            dispatcher.dispatchDue();

            assertThat(last(email).html()).doesNotContain("<script>").contains("&lt;script&gt;");
        }

        @Test
        void anUnknownVariableIsRejectedAndOnlyTheOwnerEdits() {
            assertThat(api.put(owner, business("/message-templates/APPOINTMENT_BOOKED"), """
                            {"subject":"Hola","body":"Hola {cliente}"}"""))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("unknown_template_variable");
            assertThat(api.put(manager, business("/message-templates/APPOINTMENT_BOOKED"), """
                            {"subject":"Hola","body":"Hola {nombre}"}"""))
                    .hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.get(manager, business("/message-templates"))).hasStatusOk();
            assertThat(api.get(barber, business("/message-templates"))).hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void resettingGoesBackToTheDefaultText() {
            api.put(owner, business("/message-templates/APPOINTMENT_REMINDER"), """
                    {"subject":"Mañana te vemos","body":"Hola {nombre}"}""");

            assertThat(api.delete(owner, business("/message-templates/APPOINTMENT_REMINDER")))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.subject")
                    .isEqualTo("Recordatorio: tu turno en {negocio}");
            assertThat(api.get(owner, business("/message-templates")))
                    .bodyJson()
                    .extractingPath("$.templates[?(@.type == 'APPOINTMENT_REMINDER')].custom")
                    .isEqualTo(List.of(false));
        }

        @Test
        void anotherBusinessCannotSeeTheTexts() {
            var stranger = api.registerNewUser("Dueño de otro negocio");
            api.createBusiness(stranger, "otro-" + UUID.randomUUID().toString().substring(0, 8));

            assertThat(api.get(stranger, business("/message-templates"))).hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    class Notices {

        @Test
        void eachOneMarksTheirOwnNoticesAsRead() {
            book(manager, barber, "10:00", newEmail(), true);
            book(manager, barber, "11:00", newEmail(), true);
            String noticeId = ApiClient.read(api.get(barber, business("/notices")), "$.items[0].id");

            assertThat(api.post(other, business("/notices/" + noticeId + "/read"), "{}"))
                    .hasStatus(HttpStatus.NOT_FOUND);
            assertThat(api.post(barber, business("/notices/" + noticeId + "/read"), "{}"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.read")
                    .isEqualTo(true);
            assertThat(unread(barber)).isEqualTo(1);

            assertThat(api.post(barber, business("/notices/read-all"), "{}")).hasStatus(HttpStatus.NO_CONTENT);
            assertThat(unread(barber)).isZero();
        }
    }

    // --- Ayudantes ---

    /** Carga un turno desde el local y devuelve su id. Sin email, el cliente deja solo su teléfono. */
    private String book(Session actor, Session with, String time, String email, boolean confirmed) {
        String contact = email == null ? "\"phone\":\"11 4567-8901\"" : "\"email\":\"%s\"".formatted(email);
        var result = api.post(actor, business("/appointments"), """
                {"branchId":"%s","barberId":"%s","serviceId":"%s","startsAt":"%s","confirmed":%s,
                 "customer":{"name":"Cliente de mostrador",%s}}""".formatted(
                        centro, with.userId(), corte, at(time), confirmed, contact));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return ApiClient.read(result, "$.id");
    }

    private OutgoingMessage last(String email) {
        var messages = channel.sentTo(email);
        assertThat(messages).as("emails a " + email).isNotEmpty();
        return messages.getLast();
    }

    private MvcTestResult log(String appointmentId) {
        return log(appointmentId, owner);
    }

    private MvcTestResult log(String appointmentId, Session session) {
        return api.get(session, business("/appointments/" + appointmentId + "/notifications"));
    }

    /** Estados de los avisos de ese tipo y destinatario, en orden de creación. */
    private List<String> statuses(String appointmentId, String type, String audience) {
        return JsonPath.read(
                content(log(appointmentId)),
                "$[?(@.type == '%s' && @.audience == '%s')].status".formatted(type, audience));
    }

    private long unread(Session session) {
        return Long.parseLong(ApiClient.read(api.get(session, business("/notices/unread-count")), "$.unread"));
    }

    private String business(String path) {
        return "/api/businesses/" + businessId + path;
    }

    private Instant at(String time) {
        return ZonedDateTime.of(day, LocalTime.parse(time), BUENOS_AIRES).toInstant();
    }

    private static String newEmail() {
        return "cliente-" + UUID.randomUUID() + "@example.com";
    }

    private static String tokenJson(String token) {
        return "{\"token\":\"%s\"}".formatted(token);
    }

    private static String content(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
