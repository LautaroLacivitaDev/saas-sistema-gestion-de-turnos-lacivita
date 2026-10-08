package com.lacivita.turnos.shared.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.ZoneId;

/** Guarda una zona horaria con su identificador IANA (por ejemplo {@code America/Argentina/Buenos_Aires}). */
@Converter(autoApply = true)
public class ZoneIdConverter implements AttributeConverter<ZoneId, String> {

    @Override
    public String convertToDatabaseColumn(ZoneId zone) {
        return zone == null ? null : zone.getId();
    }

    @Override
    public ZoneId convertToEntityAttribute(String value) {
        return value == null ? null : ZoneId.of(value);
    }
}
