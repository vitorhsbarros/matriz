package matriz;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * The session cookie is Secure unless MATRIZ_COOKIE_SECURE says otherwise (SEC06, SEC08). Read from
 * application.yaml itself, so a developer's .env (which may turn it off for Safari on localhost) doesn't matter.
 */
class CookieDefaultsTest {

    @Test
    void theSessionCookieIsSecureByDefault() throws IOException {
        PropertySource<?> yaml = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yaml")).get(0);

        assertThat(yaml.getProperty("server.servlet.session.cookie.secure")).hasToString("${MATRIZ_COOKIE_SECURE:true}");
        assertThat(yaml.getProperty("server.servlet.session.cookie.http-only")).hasToString("true");
        assertThat(yaml.getProperty("server.servlet.session.cookie.same-site")).hasToString("lax");
    }
}
