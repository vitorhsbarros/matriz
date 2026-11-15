package matriz;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * The liveness and readiness checks can be read without a session, and show only their status. The other
 * management endpoints stay closed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class HealthChecksTest {

    @LocalServerPort
    int port;

    private ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
    }

    @ParameterizedTest(name = "{0} is public and shows only its status")
    @ValueSource(strings = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
    void aHealthCheckIsPublicAndShowsOnlyItsStatus(String path) {
        ApiClient.Response response = api.send("GET", path, null, null, null);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().get("status").asString()).isEqualTo("UP");
        assertThat(response.json().has("components")).as("components").isFalse();
        assertThat(response.json().has("details")).as("details").isFalse();
    }

    @ParameterizedTest(name = "{0} can't be read")
    @ValueSource(strings = {"/actuator", "/actuator/info", "/actuator/env", "/actuator/health/db",
            "/actuator/health/diskSpace"})
    void otherManagementEndpointsCantBeRead(String path) {
        ApiClient.Response response = api.send("GET", path, null, null, null);

        assertThat(response.status()).isEqualTo(401);
    }
}
