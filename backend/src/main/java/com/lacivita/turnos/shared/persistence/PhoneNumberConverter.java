package com.lacivita.turnos.shared.persistence;

import com.lacivita.turnos.shared.domain.PhoneNumber;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PhoneNumberConverter implements AttributeConverter<PhoneNumber, String> {

    @Override
    public String convertToDatabaseColumn(PhoneNumber phone) {
        return phone == null ? null : phone.value();
    }

    @Override
    public PhoneNumber convertToEntityAttribute(String value) {
        return value == null ? null : new PhoneNumber(value);
    }
}
