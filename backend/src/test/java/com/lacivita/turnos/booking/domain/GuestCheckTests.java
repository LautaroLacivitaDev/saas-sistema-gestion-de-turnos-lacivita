package com.lacivita.turnos.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lacivita.turnos.catalog.BookableItem;
import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.Money;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GuestCheckTests {

    static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    static final Customer.Contact GUEST = new Customer.Contact("Invitada", new Email("invitada@example.com"), null);

    @Test
    void theCodeFromTheEmailConfirmsWhoIsBooking() {
        var check = GuestCheck.forHold(hold());
        String code = check.issueCode(GUEST, NOW);

        assertThat(code).hasSize(6).containsOnlyDigits();
        assertThat(check.matches(code, NOW.plusSeconds(30))).isTrue();
        assertThat(check.contact()).isEqualTo(GUEST);
    }

    @Test
    void afterFiveWrongAttemptsNotEvenTheRightCodeWorks() {
        var check = GuestCheck.forHold(hold());
        String code = check.issueCode(GUEST, NOW);
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            assertThat(check.matches(wrong, NOW)).isFalse();
        }

        assertThat(check.hasAttemptsLeft()).isFalse();
        assertThat(check.matches(code, NOW)).isFalse();
    }

    @Test
    void theCodeExpiresAfterTenMinutesAndANewOneReplacesIt() {
        var check = GuestCheck.forHold(hold());
        String first = check.issueCode(GUEST, NOW);

        assertThat(check.matches(first, NOW.plus(Duration.ofMinutes(10)))).isFalse();

        String second = check.issueCode(GUEST, NOW.plus(Duration.ofMinutes(11)));
        assertThat(check.matches(second, NOW.plus(Duration.ofMinutes(12)))).isTrue();
    }

    private static Appointment hold() {
        var line = new AppointmentLine(UUID.randomUUID(), "Corte", Money.of("8000"), 30);
        return Appointment.hold(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BookableItem.ServiceItem(line.serviceId()),
                List.of(line),
                NOW.plus(Duration.ofDays(1)),
                NOW);
    }
}
