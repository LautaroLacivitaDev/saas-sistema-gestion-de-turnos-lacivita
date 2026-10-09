package com.lacivita.turnos.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReminderScheduleTests {

    static final Instant START = Instant.parse("2026-10-12T13:00:00Z");

    @Test
    void remindersGoOutTheChosenHoursBeforeTheAppointment() {
        var schedule = new ReminderSchedule(List.of(2, 24));

        assertThat(schedule.hoursBefore()).containsExactly(24, 2);
        assertThat(schedule.sendTimes(START, START.minus(Duration.ofDays(3))))
                .containsExactly(START.minus(Duration.ofHours(24)), START.minus(Duration.ofHours(2)));
    }

    @Test
    void aReminderThatWouldAlreadyHavePassedIsLeftOut() {
        var bookedTenHoursBefore = START.minus(Duration.ofHours(10));

        assertThat(ReminderSchedule.DEFAULT.sendTimes(START, bookedTenHoursBefore))
                .containsExactly(START.minus(Duration.ofHours(2)));
    }

    @Test
    void anEmptyListTurnsRemindersOff() {
        assertThat(new ReminderSchedule(List.of()).sendTimes(START, START.minus(Duration.ofDays(5))))
                .isEmpty();
    }

    @Test
    void upToThreeRemindersBetweenOneHourAndOneWeekWithoutRepeats() {
        assertThatThrownBy(() -> new ReminderSchedule(List.of(1, 2, 3, 4))).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ReminderSchedule(List.of(0))).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ReminderSchedule(List.of(169))).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ReminderSchedule(List.of(2, 2))).isInstanceOf(InvalidValueException.class);
    }
}
