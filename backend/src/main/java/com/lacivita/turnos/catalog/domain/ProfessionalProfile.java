package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

/**
 * Lo que un profesional cuenta de sí en la página pública del negocio.
 *
 * @param bio descripción breve; nula si no escribió nada
 * @param specialties hasta 6, por ejemplo "Fade" o "Barba", sin repetir
 * @param photoUrl dirección https de su foto; nula para mostrar sus iniciales
 */
// DECISIÓN: la foto es un link https (por ejemplo, a su foto de perfil). Subir archivos requiere un
// almacenamiento propio (S3 o similar) y queda para más adelante.
public record ProfessionalProfile(String bio, List<String> specialties, String photoUrl) {

    static final int MAX_BIO = 500;
    static final int MAX_SPECIALTIES = 6;
    static final int MAX_SPECIALTY = 40;
    static final int MAX_URL = 500;

    public static final ProfessionalProfile EMPTY = new ProfessionalProfile(null, List.of(), null);

    public ProfessionalProfile {
        bio = optional(bio);
        if (bio != null && bio.length() > MAX_BIO) {
            throw new InvalidValueException("invalid_bio", "La descripción puede tener hasta 500 caracteres.");
        }
        specialties = specialties == null
                ? List.of()
                : specialties.stream().map(ProfessionalProfile::specialty).toList();
        if (specialties.size() > MAX_SPECIALTIES) {
            throw new InvalidValueException("invalid_specialties", "Podés cargar hasta 6 especialidades.");
        }
        long distinct = specialties.stream()
                .map(specialty -> specialty.toLowerCase(Locale.ROOT))
                .distinct()
                .count();
        if (distinct != specialties.size()) {
            throw new InvalidValueException("invalid_specialties", "Hay especialidades repetidas.");
        }
        photoUrl = photo(optional(photoUrl));
    }

    private static String specialty(String value) {
        String trimmed = optional(value);
        if (trimmed == null || trimmed.length() > MAX_SPECIALTY) {
            throw new InvalidValueException(
                    "invalid_specialties", "Cada especialidad tiene que tener entre 1 y 40 caracteres.");
        }
        return trimmed;
    }

    /** Solo https: la página pública no carga imágenes por conexiones sin cifrar. */
    private static String photo(String url) {
        if (url == null) {
            return null;
        }
        try {
            var uri = new URI(url);
            if (url.length() > MAX_URL || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw invalidPhoto();
            }
            return url;
        } catch (URISyntaxException ex) {
            throw invalidPhoto();
        }
    }

    private static InvalidValueException invalidPhoto() {
        return new InvalidValueException(
                "invalid_photo_url", "La foto tiene que ser un link que empiece con https://.");
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
