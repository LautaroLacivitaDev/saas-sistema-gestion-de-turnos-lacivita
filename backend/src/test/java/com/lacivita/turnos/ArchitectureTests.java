package com.lacivita.turnos;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Reglas de arquitectura que Spring Modulith no cubre. Los límites entre módulos los verifica {@link
 * ModularityTests}; acá se verifican las capas dentro de cada módulo:
 *
 * <pre>
 *   web  ──►  application  ──►  domain  ◄──  infrastructure
 * </pre>
 *
 * El dominio no depende de ninguna otra capa. La infraestructura implementa puertos del dominio y nadie
 * la referencia directamente.
 */
@AnalyzeClasses(packages = "com.lacivita.turnos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

    private static final String DOMAIN = "..turnos.*.domain..";
    private static final String APPLICATION = "..turnos.*.application..";
    private static final String WEB = "..turnos.*.web..";
    private static final String INFRASTRUCTURE = "..turnos.*.infrastructure..";

    @ArchTest
    static final ArchRule domainDependsOnNoOtherLayer = noClasses()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(APPLICATION, WEB, INFRASTRUCTURE)
            .because("el dominio es el centro: las demás capas dependen de él, no al revés");

    @ArchTest
    static final ArchRule applicationDoesNotKnowTheWebOrTheInfrastructure = noClasses()
            .that()
            .resideInAPackage(APPLICATION)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(WEB, INFRASTRUCTURE)
            .because("los casos de uso no dependen de cómo se los llama ni de cómo se implementan los puertos")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule infrastructureIsOnlyReachedThroughDomainPorts = noClasses()
            .that()
            .resideOutsideOfPackage(INFRASTRUCTURE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(INFRASTRUCTURE)
            .because("la infraestructura implementa interfaces del dominio y se inyecta por ellas");

    @ArchTest
    static final ArchRule domainAndApplicationDoNotDependOnHttp = noClasses()
            .that()
            .resideInAnyPackage(DOMAIN, APPLICATION)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("jakarta.servlet..", "org.springframework.web..")
            .because("HTTP es un detalle de la capa web");

    @ArchTest
    static final ArchRule webDoesNotExposeEntities = noClasses()
            .that()
            .resideInAPackage(WEB)
            .should()
            .dependOnClassesThat()
            .areAnnotatedWith(Entity.class)
            .because("la API responde con DTOs, nunca con entidades JPA");

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
