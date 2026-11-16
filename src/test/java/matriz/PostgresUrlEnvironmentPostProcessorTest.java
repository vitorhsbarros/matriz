package matriz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

/**
 * The database URL can be a JDBC URL, as before, or a PostgreSQL connection string such as the one a hosting
 * platform gives, which becomes a JDBC URL without its credentials.
 */
class PostgresUrlEnvironmentPostProcessorTest {

    private final PostgresUrlEnvironmentPostProcessor processor = new PostgresUrlEnvironmentPostProcessor();

    @Test
    void aJdbcUrlIsLeftAsItIs() {
        MockEnvironment environment = environment("jdbc:postgresql://localhost:5432/matriz");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://localhost:5432/matriz");
        assertThat(environment.getPropertySources().contains(PostgresUrlEnvironmentPostProcessor.SOURCE)).isFalse();
    }

    @Test
    void anEmptyUrlIsLeftAsItIs() {
        MockEnvironment environment = environment("");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url")).isEmpty();
    }

    @Test
    void aConnectionStringWithoutAPortBecomesAJdbcUrlOnTheDefaultPort() {
        MockEnvironment environment = environment("postgresql://matriz_user:s3cret@dpg-example-a/matriz_db");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://dpg-example-a:5432/matriz_db");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("matriz_user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("s3cret");
    }

    @Test
    void thePortAndTheQueryAreKept() {
        MockEnvironment environment = environment("postgres://u:p@db.example.com:6543/app?sslmode=require");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.example.com:6543/app?sslmode=require");
    }

    @Test
    void theUserAndPasswordAlreadySetWin() {
        MockEnvironment environment = environment("postgresql://from_url:url_password@host/db")
                .withProperty("spring.datasource.username", "configured_user")
                .withProperty("spring.datasource.password", "configured_password");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://host:5432/db");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("configured_user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("configured_password");
    }

    @Test
    void anEncodedPasswordIsDecoded() {
        MockEnvironment environment = environment("postgresql://user:p%40ss%3Aword@host/db");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("p@ss:word");
    }

    @Test
    void aConnectionStringWithoutCredentialsKeepsTheConfiguredOnes() {
        MockEnvironment environment = environment("postgresql://host/db")
                .withProperty("spring.datasource.username", "configured_user");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://host:5432/db");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("configured_user");
        assertThat(environment.getProperty("spring.datasource.password")).isNull();
    }

    @ParameterizedTest(name = "\"{0}\" is refused")
    @ValueSource(strings = {"postgresql://user:hidden-password@/db", "postgresql://user:hidden-password@host",
            "postgresql://user:hidden-password@host/", "postgresql://user:hidden password@host/db"})
    void anInvalidConnectionStringIsRefusedWithoutShowingIt(String url) {
        IllegalStateException refusal = catchThrowableOfType(IllegalStateException.class,
                () -> processor.postProcessEnvironment(environment(url), new SpringApplication()));

        assertThat(refusal).isNotNull();
        assertThat(refusal.getMessage()).contains("MATRIZ_DB_URL").doesNotContain("hidden");
    }

    private static MockEnvironment environment(String url) {
        return new MockEnvironment().withProperty("spring.datasource.url", url);
    }
}
