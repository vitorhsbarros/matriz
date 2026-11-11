package matriz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Matriz: one deployable with four business modules (ADR-0001).
 *
 * <p>Each top-level package is a module: {@code accounts}, {@code projects}, {@code teams} and
 * {@code tasks}, plus {@code web}, the shared web entry. Types directly in a module's package are
 * its public interface; sub-packages (such as {@code internal}) are private to the module. The
 * rule is checked by {@code ModularityTest} (PD03).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MatrizApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatrizApplication.class, args);
    }
}
