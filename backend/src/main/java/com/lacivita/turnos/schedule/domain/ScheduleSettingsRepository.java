package com.lacivita.turnos.schedule.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface ScheduleSettingsRepository extends Repository<ScheduleSettings, UUID> {

    ScheduleSettings save(ScheduleSettings settings);

    Optional<ScheduleSettings> findById(UUID businessId);

    /** Las reglas del negocio, o las de siempre si nunca las cambió. */
    default ScheduleRules rulesOf(UUID businessId) {
        return findById(businessId).map(ScheduleSettings::rules).orElse(ScheduleRules.DEFAULT);
    }
}
