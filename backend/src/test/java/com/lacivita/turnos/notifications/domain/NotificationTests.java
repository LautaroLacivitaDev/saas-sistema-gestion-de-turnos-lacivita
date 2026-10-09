package com.lacivita.turnos.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTests {

    static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    @Test
    void aNewNoticeIsDueRightAwayAndAReminderAtItsTime() {
        var confirmation = toCustomer(NotificationType.APPOINTMENT_BOOKED, NOW);
        var reminder = toCustomer(NotificationType.APPOINTMENT_REMINDER, NOW.plus(Duration.ofHours(20)));

        assertThat(confirmation.isDue(NOW)).isTrue();
        assertThat(reminder.isDue(NOW)).isFalse();
        assertThat(reminder.isDue(NOW.plus(Duration.ofHours(20)))).isTrue();
    }

    @Test
    void whileOneServerIsSendingItNoOtherTakesIt() {
        var notification = toCustomer(NotificationType.APPOINTMENT_BOOKED, NOW);

        notification.claim(NOW);

        assertThat(notification.isDue(NOW.plusSeconds(30))).isFalse();
        assertThat(notification.isClaimed(NOW.plusSeconds(30))).isTrue();
        // Si el servidor se cayó a mitad del envío, al vencer el plazo otro lo retoma.
        assertThat(notification.isDue(NOW.plus(Notification.LEASE))).isTrue();
    }

    @Test
    void aFailedSendIsRetriedWithGrowingWaitsUntilItGivesUp() {
        var notification = toCustomer(NotificationType.APPOINTMENT_BOOKED, NOW);
        var now = NOW;
        var waits = new Duration[] {
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofHours(1)
        };

        for (Duration wait : waits) {
            notification.claim(now);
            notification.markFailed("smtp_error", now);
            assertThat(notification.getStatus()).isEqualTo(DeliveryStatus.PENDING);
            assertThat(notification.isDue(now.plus(wait).minusSeconds(1))).isFalse();
            now = now.plus(wait);
        }
        notification.claim(now);
        notification.markFailed("smtp_error", now);

        assertThat(notification.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(notification.getAttempts()).isEqualTo(Notification.MAX_ATTEMPTS);
        assertThat(notification.lastError()).contains("smtp_error");
    }

    @Test
    void aSentNoticeRecordsWhenItWentOut() {
        var notification = toCustomer(NotificationType.APPOINTMENT_BOOKED, NOW);
        notification.claim(NOW);

        notification.markSent(NOW.plusSeconds(2));

        assertThat(notification.getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(notification.sentAt()).contains(NOW.plusSeconds(2));
        assertThat(notification.getAttempts()).isOne();
    }

    @Test
    void onlyWhatIsStillPendingCanBeCancelled() {
        var sent = toCustomer(NotificationType.APPOINTMENT_REMINDER, NOW);
        sent.claim(NOW);
        sent.markSent(NOW);
        var pending = toCustomer(NotificationType.APPOINTMENT_REMINDER, NOW.plus(Duration.ofHours(2)));

        sent.cancel(NOW);
        pending.cancel(NOW);

        assertThat(sent.getStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(pending.getStatus()).isEqualTo(DeliveryStatus.CANCELLED);
    }

    @Test
    void barbersGetNoticesOfChangesButNotReminders() {
        assertThatThrownBy(() -> Notification.toBarber(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        NotificationType.APPOINTMENT_REMINDER,
                        NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Notification toCustomer(NotificationType type, Instant dueAt) {
        return Notification.toCustomer(UUID.randomUUID(), UUID.randomUUID(), type, dueAt, NOW);
    }
}
