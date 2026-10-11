package com.genai.tasks;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the Clean Architecture dependency rule: dependencies point inward
 * (web / infrastructure -> application -> domain) and the inner layers stay framework-free.
 */
@AnalyzeClasses(packages = "com.genai.tasks", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnNothingButTheJdk = classes()
            .that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "..domain..")
            .as("domain must not import any framework or outer layer");

    @ArchTest
    static final ArchRule applicationDependsOnlyOnDomainAndJdk = classes()
            .that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "..domain..", "..application..")
            .as("application (use cases + ports) must not import any framework or outer layer");

    @ArchTest
    static final ArchRule infrastructureDoesNotDependOnWeb = noClasses()
            .that().resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("com.genai.tasks..web..");

    @ArchTest
    static final ArchRule webNeverTouchesJpaEntities = noClasses()
            .that().resideInAPackage("..web..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
            .as("controllers must never expose JPA entities");

    @ArchTest
    static final ArchRule taskModuleIsIndependentOfUserModule = noClasses()
            .that().resideInAPackage("com.genai.tasks.task..")
            .should().dependOnClassesThat().resideInAPackage("com.genai.tasks.user..");

    @ArchTest
    static final ArchRule userModuleIsIndependentOfTaskModule = noClasses()
            .that().resideInAPackage("com.genai.tasks.user..")
            .should().dependOnClassesThat().resideInAPackage("com.genai.tasks.task..");
}
