package com.lacivita.turnos.business.application;

import com.lacivita.turnos.business.domain.Business;
import com.lacivita.turnos.business.domain.BusinessRepository;
import com.lacivita.turnos.business.domain.SlugClaimRepository;
import com.lacivita.turnos.business.domain.SlugTakenException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Garantiza que un slug no lo tome dos veces: primero lo verifica y, si dos negocios lo piden a la
 * vez, decide la restricción única de la base. Por eso escribe enseguida, dentro del caso de uso.
 */
@Component
class SlugGuard {

    private static final String SLUG_CONSTRAINTS_PREFIX = "business_slug_";

    private final SlugClaimRepository slugClaims;
    private final BusinessRepository businesses;

    SlugGuard(SlugClaimRepository slugClaims, BusinessRepository businesses) {
        this.slugClaims = slugClaims;
        this.businesses = businesses;
    }

    boolean isTaken(String slug) {
        return slugClaims.existsById(slug);
    }

    /** Guarda un negocio nuevo junto con su primer slug. */
    void saveNew(Business business) {
        translatingSlugConflicts(() -> businesses.saveAndFlush(business));
    }

    /** Escribe el slug nuevo de un negocio ya cargado (se guarda en cascada con el negocio). */
    void flushClaims() {
        translatingSlugConflicts(businesses::flush);
    }

    private static void translatingSlugConflicts(Runnable write) {
        try {
            write.run();
        } catch (DataIntegrityViolationException ex) {
            if (isSlugConflict(ex)) {
                throw new SlugTakenException();
            }
            throw ex;
        }
    }

    private static boolean isSlugConflict(DataIntegrityViolationException ex) {
        // Cubre la clave primaria de business_slug (business_slug_pkey) y el único de business (business_slug_uk).
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.contains(SLUG_CONSTRAINTS_PREFIX);
    }
}
