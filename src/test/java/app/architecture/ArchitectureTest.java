package app.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("app");
    }

    @Test
    void uiDoesNotImportCloudConvert() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..ui..")
                .should().dependOnClassesThat().resideInAPackage("..cloudconvert..")
                .orShould().dependOnClassesThat().resideInAPackage("com.cloudconvert..");
        rule.check(classes);
    }

    @Test
    void coreDoesNotImportJavaFX() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..core..")
                .should().dependOnClassesThat().resideInAPackage("javafx..");
        rule.check(classes);
    }

    @Test
    void persistenceDoesNotImportCoreBusinessLogic() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..persistence..")
                .should().dependOnClassesThat().resideInAPackage("..core..");
        rule.check(classes);
    }

    @Test
    void noCircularDependencies() {
        ArchRule rule = slices()
                .matching("app.(*)..")
                .should().beFreeOfCycles();
        rule.check(classes);
    }
}
