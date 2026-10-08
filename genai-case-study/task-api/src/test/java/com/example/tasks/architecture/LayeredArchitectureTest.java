package com.example.tasks.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(packages = "com.example.tasks", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String INFRASTRUCTURE = "..infrastructure..";
    private static final String INTERFACES = "..interfaces..";
    private static final String TRANSACTION = "..infrastructure.transaction..";

    private static final DescribedPredicate<JavaClass> ARE_RECORDS =
        new DescribedPredicate<>("are records") {
            @Override
            public boolean test(JavaClass javaClass) {
                return javaClass.isRecord();
            }
        };

    // ---- framework-free core (allowlists) -------------------------------------------------------

    @ArchTest
    static final ArchRule domain_depends_only_on_java_and_itself =
        classes().that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN);

    @ArchTest
    static final ArchRule application_depends_only_on_java_domain_and_itself =
        classes().that().resideInAPackage(APPLICATION)
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", DOMAIN, APPLICATION);

    // ---- adapters don't know each other -------------------------------------------------------

    @ArchTest
    static final ArchRule interfaces_must_not_depend_on_infrastructure =
        noClasses().that().resideInAPackage(INTERFACES)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE);

    @ArchTest
    static final ArchRule infrastructure_must_not_depend_on_interfaces =
        noClasses().that().resideInAPackage(INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAPackage(INTERFACES);

    // ---- input ports ----------------------------------------------------------------------------

    @ArchTest
    static final ArchRule interfaces_depend_on_input_ports_not_interactors =
        noClasses().that().resideInAPackage(INTERFACES)
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Interactor");

    @ArchTest
    static final ArchRule interactors_live_in_usecase_and_implement_an_input_port =
        classes().that().haveSimpleNameEndingWith("Interactor")
            .should().resideInAPackage("..application.usecase..")
            .andShould().implement(JavaClass.Predicates.simpleNameEndingWith("UseCase"));

    @ArchTest
    static final ArchRule input_ports_are_interfaces =
        classes().that().resideInAPackage("..application.usecase..")
            .and().haveSimpleNameEndingWith("UseCase")
            .should().beInterfaces();

    // ---- persistence ----------------------------------------------------------------------------

    @ArchTest
    static final ArchRule jpa_entities_live_only_in_persistence_entity =
        classes().that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..infrastructure.persistence.entity..");

    @ArchTest
    static final ArchRule no_cross_aggregate_jpa_associations =
        noFields().should().beAnnotatedWith(ManyToOne.class)
            .orShould().beAnnotatedWith(OneToOne.class)
            .orShould().beAnnotatedWith(ManyToMany.class);

    @ArchTest
    static final ArchRule transactional_classes_only_in_the_unit_of_work_adapter =
        noClasses().that().resideOutsideOfPackage(TRANSACTION)
            .should().beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule transactional_methods_only_in_the_unit_of_work_adapter =
        noMethods().that().areDeclaredInClassesThat().resideOutsideOfPackage(TRANSACTION)
            .should().beAnnotatedWith(Transactional.class);

    // ---- determinism ----------------------------------------------------------------------------

    // The no-arg overloads only: Instant.now(clock) is the allowed form.
    @ArchTest
    static final ArchRule clock_read_only_at_the_edge =
        noClasses().that().resideOutsideOfPackage(INTERFACES)
            .should().callMethod(Instant.class, "now")
            .orShould().callMethod(LocalDate.class, "now");

    @ArchTest
    static final ArchRule random_uuid_only_inside_domain_model =
        noClasses().that().resideOutsideOfPackage("..domain.model..")
            .should().callMethod(UUID.class, "randomUUID");

    // ---- hygiene --------------------------------------------------------------------------------

    @ArchTest
    static final ArchRule no_optional_get =
        noClasses().should().callMethod(Optional.class, "get");

    @ArchTest
    static final ArchRule no_field_injection =
        noFields().should().beAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule no_setters_in_domain =
        noMethods().that().haveNameStartingWith("set")
            .should().beDeclaredInClassesThat().resideInAPackage(DOMAIN);

    @ArchTest
    static final ArchRule domain_entities_have_only_private_constructors =
        classes().that().resideInAPackage("..domain.model..").and(ARE_RECORDS.negate())
            .should().haveOnlyPrivateConstructors();
}
