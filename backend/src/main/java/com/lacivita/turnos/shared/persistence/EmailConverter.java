package com.lacivita.turnos.shared.persistence;

import com.lacivita.turnos.shared.domain.Email;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el objeto de valor {@link Email} como texto. Se aplica a todos los atributos de tipo Email. */
@Converter(autoApply = true)
public class EmailConverter implements AttributeConverter<Email, String> {

    @Override
    public String convertToDatabaseColumn(Email email) {
        return email == null ? null : email.value();
    }

    @Override
    public Email convertToEntityAttribute(String value) {
        return value == null ? null : new Email(value);
    }
}
