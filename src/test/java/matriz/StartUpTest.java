package matriz;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

/**
 * The application starts against PostgreSQL, reports the database as up, serves the browser
 * application, and asks for log-in on the API (I1).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class StartUpTest {

    @LocalServerPort
    int port;

    @Test
    void healthCheckReportsTheDatabaseAsUp() {
        String health = client().get().uri("/actuator/health").retrieve().body(String.class);

        assertThat(health).contains("\"status\":\"UP\"");
    }

    @Test
    void theApiAsksForLogInWithoutASession() {
        int status = client().get().uri("/api/projects")
                .exchange((request, response) -> response.getStatusCode().value());

        assertThat(status).isEqualTo(401);
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }
}
