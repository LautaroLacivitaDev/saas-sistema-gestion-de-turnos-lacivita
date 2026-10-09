package com.lacivita.turnos.booking.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface BookingSettingsRepository extends Repository<BookingSettings, UUID> {

    BookingSettings save(BookingSettings settings);

    Optional<BookingSettings> findById(UUID businessId);

    default CancellationPolicy cancellationOf(UUID businessId) {
        return findById(businessId).map(BookingSettings::cancellation).orElse(CancellationPolicy.DEFAULT);
    }
}
