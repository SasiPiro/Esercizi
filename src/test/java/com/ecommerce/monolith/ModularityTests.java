package com.ecommerce.monolith;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Module boundaries are a rule checked like a compile error: a module may use only the
 * root package (the API) of another module, never its internal package, and modules may
 * not depend on each other in a cycle.
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(EcommerceMonolithApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        modules.verify();
    }

    @Test
    void writeDocumentation() {
        // C4 and PlantUML module diagrams and module canvases in target/spring-modulith-docs
        new Documenter(modules).writeDocumentation();
    }
}
