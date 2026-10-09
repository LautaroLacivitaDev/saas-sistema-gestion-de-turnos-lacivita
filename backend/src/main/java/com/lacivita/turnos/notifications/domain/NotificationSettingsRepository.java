package com.lacivita.turnos.notifications.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface NotificationSettingsRepository extends Repository<NotificationSettings, UUID> {

    NotificationSettings save(NotificationSettings settings);

    Optional<NotificationSettings> findById(UUID businessId);

    default ReminderSchedule remindersOf(UUID businessId) {
        return findById(businessId).map(NotificationSettings::reminders).orElse(ReminderSchedule.DEFAULT);
    }
}
