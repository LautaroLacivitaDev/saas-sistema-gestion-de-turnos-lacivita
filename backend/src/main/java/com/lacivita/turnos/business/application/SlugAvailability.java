package com.lacivita.turnos.business.application;

import com.lacivita.turnos.business.application.BusinessViews.SlugCheck;
import com.lacivita.turnos.business.domain.Slug;
import com.lacivita.turnos.business.domain.SlugTakenException;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verificación en tiempo real de un slug mientras la persona lo escribe. */
@Service
public class SlugAvailability {

    private final SlugGuard slugs;

    SlugAvailability(SlugGuard slugs) {
        this.slugs = slugs;
    }

    @Transactional(readOnly = true)
    public SlugCheck check(String candidate) {
        Slug slug;
        try {
            slug = new Slug(candidate);
        } catch (InvalidValueException ex) {
            return SlugCheck.unavailable(candidate, ex.code(), ex.getMessage());
        }
        if (slugs.isTaken(slug.value())) {
            var taken = new SlugTakenException();
            return SlugCheck.unavailable(slug.value(), taken.code(), taken.getMessage());
        }
        return SlugCheck.available(slug.value());
    }
}
