package com.restaurant.saas.architecture;

import com.restaurant.saas.common.entity.BaseEntity;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.restaurant.saas");
    }

    @Test
    void allEntitiesExtendBaseEntity() {
        ArchRule rule = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().beAssignableTo(BaseEntity.class)
            .because("every aggregate must carry audit and soft-delete fields");
        rule.check(classes);
    }

    @Test
    void allEntitiesDeclareSqlRestriction() {
        ArchRule rule = classes()
            .that().areAnnotatedWith(Entity.class)
            .should(new ArchCondition<>("declare @SQLRestriction filtering soft-deleted rows") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    boolean hasRestriction = item.tryGetAnnotationOfType(SQLRestriction.class).isPresent();
                    if (!hasRestriction) {
                        events.add(SimpleConditionEvent.violated(
                            item, item.getName() + " is missing @SQLRestriction"));
                    }
                }
            });
        rule.check(classes);
    }

    @Test
    void entitiesDoNotOverrideEqualsOrHashCode() {
        ArchRule rule = classes()
            .that().areAnnotatedWith(Entity.class)
            .should(new ArchCondition<>("rely on BaseEntity identity, not own equals/hashCode") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    boolean overrides = item.getMethods().stream()
                        .filter(method -> method.getOwner().equals(item))
                        .anyMatch(method -> method.getName().equals("equals")
                            || method.getName().equals("hashCode"));
                    if (overrides) {
                        events.add(SimpleConditionEvent.violated(
                            item, item.getName() + " declares equals/hashCode; identity is defined by BaseEntity"));
                    }
                }
            });
        rule.check(classes);
    }

    @Test
    void controllersLiveInWebPackages() {
        ArchRule rule = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..web..")
            .orShould().resideInAPackage("..web");
        rule.check(classes);
    }

    @Test
    void everyEntityHasTableAnnotation() {
        classes.stream()
            .filter(javaClass -> javaClass.isAnnotatedWith(Entity.class))
            .forEach(javaClass -> assertThat(javaClass.tryGetAnnotationOfType(Table.class))
                .as(javaClass.getName())
                .isPresent());
    }
}
