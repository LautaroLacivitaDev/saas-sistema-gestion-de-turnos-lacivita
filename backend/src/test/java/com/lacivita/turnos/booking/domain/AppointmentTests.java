package com.lacivita.turnos.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.catalog.BookableItem;
import com.lacivita.turnos.shared.domain.Money;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AppointmentTests {

    static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    static final Instant START = Instant.parse("2026-10-12T12:00:00Z");
    static final UUID BUSINESS = UUID.randomUUID();
    static final UUID BRANCH = UUID.randomUUID();
    static final UUID BARBER = UUID.randomUUID();
    static final UUID CUSTOMER = UUID.randomUUID();
    static final AppointmentLine CORTE = new AppointmentLine(UUID.randomUUID(), "Corte", Money.of("9000"), 30);
    static final AppointmentLine BARBA = new AppointmentLine(UUID.randomUUID(), "Barba", Money.of("5000"), 20);

    @Nested
    class OnlineHold {

        @Test
        void aHoldKeepsTheTimeForFiveMinutesWithTheCopiedPrices() {
            var hold = hold(List.of(CORTE, BARBA));

            assertThat(hold.getStatus()).isEqualTo(AppointmentStatus.HOLD);
            assertThat(hold.holdExpiresAt()).contains(NOW.plus(Duration.ofMinutes(5)));
            assertThat(hold.getTotalPrice()).isEqualTo(Money.of("14000"));
            assertThat(hold.interval().end()).isEqualTo(START.plus(Duration.ofMinutes(50)));
        }

        @Test
        void confirmingInTimeLeavesItConfirmedWithItsCustomer() {
            var hold = hold(List.of(CORTE));

            hold.confirmHold(CUSTOMER, ManageToken.generate(), NOW.plusSeconds(60));

            assertThat(hold.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
            assertThat(hold.customerId()).contains(CUSTOMER);
            assertThat(hold.holdExpiresAt()).isEmpty();
        }

        @Test
        void anExpiredHoldCannotBeConfirmed() {
            var hold = hold(List.of(CORTE));

            assertThatThrownBy(
                            () -> hold.confirmHold(CUSTOMER, ManageToken.generate(), NOW.plus(Duration.ofMinutes(5))))
                    .isInstanceOf(HoldExpiredException.class);
        }
    }

    @Nested
    class Lifecycle {

        @Test
        void aCounterBookingCanStartPendingAndThenBeConfirmed() {
            var appointment = counter(false);

            appointment.confirm(NOW);

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
            assertThat(appointment.getSource()).isEqualTo(AppointmentSource.COUNTER);
        }

        @Test
        void itIsCompletedOrMarkedAsNoShowOnlyOnceItStarted() {
            var appointment = counter(true);

            assertThatThrownBy(() -> appointment.complete(NOW)).isInstanceOf(AppointmentStatusException.class);
            assertThatThrownBy(() -> appointment.markNoShow(NOW)).isInstanceOf(AppointmentStatusException.class);

            appointment.start(START);
            appointment.complete(START.plus(Duration.ofMinutes(30)));

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        }

        @Test
        void finishedAppointmentsCannotChangeAnymore() {
            var appointment = counter(true);
            appointment.cancel(NOW);

            assertThatThrownBy(() -> appointment.cancel(NOW)).isInstanceOf(AppointmentStatusException.class);
            assertThatThrownBy(() -> appointment.reschedule(START.plus(Duration.ofHours(1)), BARBER, NOW))
                    .isInstanceOf(AppointmentStatusException.class);
        }

        @Test
        void reschedulingKeepsTheServicesAndThePrice() {
            var appointment = counter(true);
            var otherBarber = UUID.randomUUID();
            var later = START.plus(Duration.ofHours(2));

            appointment.reschedule(later, otherBarber, NOW);

            assertThat(appointment.getBarberId()).isEqualTo(otherBarber);
            assertThat(appointment.interval().end()).isEqualTo(later.plus(Duration.ofMinutes(30)));
            assertThat(appointment.getTotalPrice()).isEqualTo(Money.of("9000"));
        }
    }

    @Nested
    class CustomerChanges {

        static final CancellationPolicy TWO_HOURS = new CancellationPolicy(2);

        @Test
        void theCustomerCancelsOrReschedulesUntilTheDeadline() {
            var appointment = counter(true);

            appointment.rescheduleByCustomer(
                    TWO_HOURS, START.plus(Duration.ofHours(1)), START.minus(Duration.ofHours(2)));
            appointment.cancelByCustomer(TWO_HOURS, START.minus(Duration.ofHours(1)));

            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        }

        @Test
        void afterTheDeadlineOnlyTheTeamCanChangeIt() {
            var appointment = counter(true);
            var lateNow = START.minus(Duration.ofMinutes(90));

            assertThatThrownBy(() -> appointment.cancelByCustomer(TWO_HOURS, lateNow))
                    .isInstanceOf(ChangeDeadlinePassedException.class);
            assertThatThrownBy(
                            () -> appointment.rescheduleByCustomer(TWO_HOURS, START.plus(Duration.ofDays(1)), lateNow))
                    .isInstanceOf(ChangeDeadlinePassedException.class);

            appointment.cancel(lateNow);
            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        }

        @Test
        void aPolicyOfZeroHoursAllowsChangesUntilTheStart() {
            var policy = new CancellationPolicy(0);

            assertThat(policy.allowsChangeAt(START, START)).isTrue();
            assertThat(policy.allowsChangeAt(START, START.plusSeconds(1))).isFalse();
        }
    }

    private static Appointment hold(List<AppointmentLine> lines) {
        var item = lines.size() > 1
                ? new BookableItem.ComboItem(UUID.randomUUID())
                : new BookableItem.ServiceItem(lines.getFirst().serviceId());
        return Appointment.hold(BUSINESS, BRANCH, BARBER, item, lines, START, NOW);
    }

    private static Appointment counter(boolean confirmed) {
        return Appointment.atCounter(
                BUSINESS,
                BRANCH,
                BARBER,
                CUSTOMER,
                new BookableItem.ServiceItem(CORTE.serviceId()),
                List.of(CORTE),
                START,
                confirmed,
                UUID.randomUUID(),
                NOW);
    }
}
