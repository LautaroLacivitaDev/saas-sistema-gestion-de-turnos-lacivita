package com.lacivita.turnos.catalog.infrastructure;

import com.lacivita.turnos.catalog.domain.ServiceDuration;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda la duración en minutos. */
@Converter(autoApply = true)
public class ServiceDurationConverter implements AttributeConverter<ServiceDuration, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ServiceDuration duration) {
        return duration == null ? null : duration.minutes();
    }

    @Override
    public ServiceDuration convertToEntityAttribute(Integer minutes) {
        return minutes == null ? null : new ServiceDuration(minutes);
    }
}
