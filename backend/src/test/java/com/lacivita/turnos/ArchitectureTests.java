package com.lacivita.turnos;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;

/** Reglas de arquitectura que Spring Modulith no cubre. */
@AnalyzeClasses(packages = "com.lacivita.turnos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

    @ArchTest
    static final ArchRule sharedDoesNotDependOnBusinessModules = noClasses()
            .that()
            .resideInAPackage("..turnos.shared..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..turnos.business..",
                    "..turnos.users..",
                    "..turnos.catalog..",
                    "..turnos.schedule..",
                    "..turnos.booking..",
                    "..turnos.notifications..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule noFieldInjection = noFields()
            .should()
            .beAnnotatedWith(Autowired.class)
            .because("las dependencias se inyectan por constructor")
            .allowEmptyShould(true);
}
