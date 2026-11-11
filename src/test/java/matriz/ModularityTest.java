package matriz;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Module boundaries (ADR-0001, PD03): no module uses another module's internal types, and there
 * are no dependency cycles. Fails the build when a rule is broken.
 */
class ModularityTest {

    @Test
    void modulesRespectTheirBoundaries() {
        ApplicationModules.of(MatrizApplication.class).verify();
    }
}
