package com.lacivita.turnos.schedule.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WeeklyHoursTests {

    @Test
    void rangesOfADayAreSortedAndDaysWithoutRangesAreClosed() {
        var hours = new WeeklyHours(Map.of(
                DayOfWeek.MONDAY, List.of(DayRange.of("14:00", "19:00"), DayRange.of("09:00", "13:00")),
                DayOfWeek.SUNDAY, List.of()));

        assertThat(hours.on(DayOfWeek.MONDAY))
                .containsExactly(DayRange.of("09:00", "13:00"), DayRange.of("14:00", "19:00"));
        assertThat(hours.on(DayOfWeek.SUNDAY)).isEmpty();
        assertThat(hours.days()).containsOnlyKeys(DayOfWeek.MONDAY);
    }

    @Test
    void rangesOfTheSameDayCannotOverlapButCanTouch() {
        assertThatThrownBy(() -> new WeeklyHours(Map.of(
                        DayOfWeek.MONDAY, List.of(DayRange.of("09:00", "13:00"), DayRange.of("12:00", "15:00")))))
                .isInstanceOf(InvalidValueException.class);
        assertThat(new WeeklyHours(Map.of(
                                DayOfWeek.MONDAY,
                                List.of(DayRange.of("09:00", "13:00"), DayRange.of("13:00", "15:00"))))
                        .on(DayOfWeek.MONDAY))
                .hasSize(2);
    }

    @Test
    void rangesGoInStepsOfFiveMinutesAndEndAfterTheyStart() {
        assertThatThrownBy(() -> DayRange.of("09:03", "10:00")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> DayRange.of("18:00", "09:00")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new DayRange(LocalTime.of(9, 0, 30), LocalTime.of(10, 0)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void theCommonPartOfTwoRangesIsTheirIntersection() {
        assertThat(DayRange.of("08:00", "13:00").intersect(DayRange.of("10:00", "18:00")))
                .contains(DayRange.of("10:00", "13:00"));
        assertThat(DayRange.of("08:00", "10:00").intersect(DayRange.of("10:00", "18:00")))
                .isEmpty();
    }

    @Test
    void scheduleRulesHaveSensibleLimits() {
        assertThatThrownBy(() -> new ScheduleRules(7, 0, 60, 15)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ScheduleRules(0, 0, 0, 15)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ScheduleRules(0, 0, 60, 7)).isInstanceOf(InvalidValueException.class);
        assertThat(ScheduleRules.DEFAULT.minNoticeMinutes()).isEqualTo(60);
    }
}
