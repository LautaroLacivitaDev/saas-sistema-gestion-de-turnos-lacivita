package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;

/**
 * Contraseña elegida por la persona, antes de pasar por el hash. Solo existe en memoria durante el
 * registro.
 *
 * <p>El máximo de 64 caracteres respeta el límite de 72 bytes de BCrypt aun con caracteres
 * acentuados.
 */
public record NewPassword(String value) {

    static final int MIN_LENGTH = 8;
    static final int MAX_LENGTH = 64;

    public NewPassword {
        if (value == null || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidValueException(
                    "invalid_password",
                    "La contraseña tiene que tener entre " + MIN_LENGTH + " y " + MAX_LENGTH + " caracteres.");
        }
        if (value.isBlank()) {
            throw new InvalidValueException("invalid_password", "La contraseña no puede ser solo espacios.");
        }
    }

    @Override
    public String toString() {
        return "NewPassword[***]";
    }
}
