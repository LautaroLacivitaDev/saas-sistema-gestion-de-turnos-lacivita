package com.lacivita.turnos;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** Verifica los límites entre módulos y genera la documentación de la arquitectura. */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(TurnosApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        modules.verify();
    }

    @Test
    void writeModuleDocumentation() {
        // Genera diagramas en target/spring-modulith-docs.
        new Documenter(modules).writeDocumentation();
    }
}
