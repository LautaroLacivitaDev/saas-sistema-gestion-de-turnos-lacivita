package com.lacivita.turnos.notifications.infrastructure;

import com.lacivita.turnos.notifications.domain.DemoEmail;
import com.lacivita.turnos.notifications.domain.DemoEmailRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Guarda los emails del modo demo y, de paso, borra los que ya vencieron. */
@Component
@ConditionalOnBooleanProperty("app.demo.enabled")
class DemoEmailStore {

    /** Lo justo para probar la app: después los emails se borran. */
    static final Duration RETENTION = Duration.ofDays(3);

    private final DemoEmailRepository emails;
    private final Clock clock;

    DemoEmailStore(DemoEmailRepository emails, Clock clock) {
        this.emails = emails;
        this.clock = clock;
    }

    /**
     * Se une a la transacción de quien manda el email: si esa transacción se revierte, el email no aparece
     * (igual que no se enviaría).
     */
    @Transactional
    void store(DemoEmail email) {
        emails.deleteOlderThan(clock.instant().minus(RETENTION));
        emails.save(email);
    }

    Clock clock() {
        return clock;
    }
}
