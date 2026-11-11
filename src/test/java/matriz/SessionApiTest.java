package matriz;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import matriz.ApiClient.LoggedIn;
import matriz.ApiClient.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Increment I1: log-in, the current session and log-out (US02, US19, DoD02, DoD06, DoD07; ADR-0002), through
 * real HTTP requests to the running application. The passwords are fictitious and exist only in the tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class SessionApiTest {

    static final String ANA = "ana@example.com";
    static final String ANA_PASSWORD = "test-password-a";

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    ApiClient api;

    @BeforeEach
    void client() {
        api = new ApiClient(port);
    }

    // ------------------------------------------------------------------ valid credentials

    @Test
    void validCredentialsLogInAndTheProfileReturnsThatUser() {
        Response logIn = api.logIn(ANA, ANA_PASSWORD);

        assertThat(logIn.status()).isEqualTo(200);
        assertThat(logIn.field("name")).isEqualTo("Ana Silva");
        assertThat(logIn.field("email")).isEqualTo(ANA);
        assertThat(logIn.csrfToken()).isNotBlank();
        assertThat(logIn.body()).doesNotContain("password");

        Response profile = api.send("GET", "/api/session", null, logIn.sessionCookie().orElseThrow(), null);

        assertThat(profile.status()).isEqualTo(200);
        assertThat(profile.field("name")).isEqualTo("Ana Silva");      // US02 AC10
        assertThat(profile.csrfToken()).isEqualTo(logIn.csrfToken());
    }

    @Test
    void theSessionCookieCantBeReadByScriptsAndIsOnlySentToTheSameSite() {
        String setCookie = api.logIn(ANA, ANA_PASSWORD).setCookie().orElseThrow();

        assertThat(setCookie).contains("HttpOnly", "Secure", "SameSite=Lax");   // SEC06
    }

    @Test
    void theEmailIsComparedIgnoringCaseAndSpaces() {
        Response logIn = api.logIn("  ANA@example.com ", ANA_PASSWORD);   // US02 AC07, D01, D03

        assertThat(logIn.status()).isEqualTo(200);
        assertThat(logIn.field("email")).isEqualTo(ANA);
    }

    @Test
    void aSessionEndsAfterThirtyMinutesWithoutRequests() {
        String cookie = api.logInAs(ANA, ANA_PASSWORD).cookie();

        List<Integer> timeouts = jdbc.sql("select max_inactive_interval from spring_session")
                .query(Integer.class).list();

        assertThat(cookie).isNotBlank();
        assertThat(timeouts).isNotEmpty().allSatisfy(seconds -> assertThat(seconds).isEqualTo(30 * 60)); // PD02
    }

    // ------------------------------------------------------------------ invalid credentials

    @Test
    void aWrongPasswordIsRefused() {
        Response logIn = api.logIn(ANA, "wrong-password");   // US02 AC03

        assertThat(logIn.status()).isEqualTo(401);
        assertThat(logIn.field("detail")).isEqualTo("Invalid email or password.");
        assertThat(logIn.setCookie()).isEmpty();
    }

    @Test
    void thePasswordIsCaseSensitive() {
        Response logIn = api.logIn(ANA, ANA_PASSWORD.toUpperCase());   // US02 AC08

        assertThat(logIn.status()).isEqualTo(401);
        assertThat(logIn.field("detail")).isEqualTo("Invalid email or password.");
    }

    @Test
    void anUnknownEmailAndAWrongPasswordGetExactlyTheSameResponse() {
        Response unknownEmail = api.logIn("nobody@example.com", "wrong-password");   // US02 AC04
        Response wrongPassword = api.logIn(ANA, "wrong-password");                   // US02 AC03

        assertThat(unknownEmail.status()).isEqualTo(401);
        assertThat(unknownEmail.body()).isEqualTo(wrongPassword.body());                    // US02 AC09, SEC04
        assertThat(headersWithoutDate(unknownEmail.raw().headers()))
                .isEqualTo(headersWithoutDate(wrongPassword.raw().headers()));
    }

    @Test
    void missingFieldsAreRefused() {
        List<String> bodies = List.of(                                                      // US02 AC05
                "{\"email\":\"\",\"password\":\"secure123\"}",
                "{\"email\":\"ana@example.com\",\"password\":\"\"}",
                "{\"email\":\"\",\"password\":\"\"}",
                "{\"email\":\"   \",\"password\":\"   \"}",                                 // D01
                "{}");

        for (String body : bodies) {
            Response logIn = api.send("POST", "/api/session", body, null, null);

            assertThat(logIn.status()).as(body).isEqualTo(422);
            assertThat(logIn.field("detail")).as(body).isEqualTo("Email and password are required.");
        }
    }

    @Test
    void aMalformedBodyIsRefused() {
        List<String> bodies = List.of(                                                      // DoD07
                "{\"email\":",
                "{\"email\":\"ana@example.com\",\"password\":\"x\",\"role\":\"MANAGER\"}",
                "{\"email\":123,\"password\":\"secure123\"}");

        for (String body : bodies) {
            Response logIn = api.send("POST", "/api/session", body, null, null);

            assertThat(logIn.status()).as(body).isEqualTo(400);
            assertThat(logIn.field("detail")).as(body).isEqualTo("The request body is not valid.");
        }
    }

    // ------------------------------------------------------------------ requests without authentication

    @Test
    void requestsWithoutASessionAreRefused() {
        List<Response> responses = List.of(                                                 // DoD02, US02 AC06
                api.send("GET", "/api/session", null, null, null),
                api.send("GET", "/api/projects", null, null, null),
                api.send("GET", "/api/teams", null, null, null),
                api.send("POST", "/api/projects", "{\"name\":\"Website\"}", null, null),
                api.send("DELETE", "/api/session", null, null, null),
                api.send("GET", "/api/session", null, "not-a-real-session", null));

        assertThat(responses).allSatisfy(response -> {
            assertThat(response.status()).isEqualTo(401);
            assertThat(response.field("detail")).isEqualTo("Please log in to continue.");
        });
    }

    // ------------------------------------------------------------------ log-out and the session's lifecycle

    @Test
    void afterLogOutTheOldSessionCookieIsRefused() {
        LoggedIn ana = api.logInAs(ANA, ANA_PASSWORD);

        Response logOut = api.send("DELETE", "/api/session", null, ana.cookie(), ana.csrfToken());

        assertThat(logOut.status()).isEqualTo(204);                                         // US02 AC02
        assertThat(logOut.setCookie()).hasValueSatisfying(header -> assertThat(header).contains("Max-Age=0"));

        Response afterLogOut = api.send("GET", "/api/session", null, ana.cookie(), null);
        Response changeAfterLogOut = api.send("DELETE", "/api/session", null, ana.cookie(), ana.csrfToken());

        assertThat(afterLogOut.status()).isEqualTo(401);
        assertThat(afterLogOut.field("detail")).isEqualTo("Please log in to continue.");
        assertThat(changeAfterLogOut.status()).isEqualTo(401);   // not "could not be verified": the session is gone
    }

    @Test
    void theSessionIdentifierChangesAtLogIn() {
        String before = api.logInAs(ANA, ANA_PASSWORD).cookie();

        // Log in again while sending the earlier session's cookie (session fixation).
        Response again = api.send("POST", "/api/session",
                "{\"email\":\"" + ANA + "\",\"password\":\"" + ANA_PASSWORD + "\"}", before, null);
        String after = again.sessionCookie().orElseThrow();

        assertThat(after).isNotEqualTo(before);
        assertThat(api.send("GET", "/api/session", null, before, null).status()).isEqualTo(401);
        assertThat(api.send("GET", "/api/session", null, after, null).status()).isEqualTo(200);
    }

    @Test
    void aChangeWithoutTheSessionsCsrfTokenIsRefused() {
        LoggedIn ana = api.logInAs(ANA, ANA_PASSWORD);
        LoggedIn rui = api.logInAs("rui@example.com", "test-password-r");

        List<Response> responses = List.of(                                                 // DoD06, SEC07
                api.send("DELETE", "/api/session", null, ana.cookie(), null),
                api.send("DELETE", "/api/session", null, ana.cookie(), "wrong-token"),
                api.send("DELETE", "/api/session", null, ana.cookie(), rui.csrfToken()));

        assertThat(responses).allSatisfy(response -> {
            assertThat(response.status()).isEqualTo(403);
            assertThat(response.field("detail"))
                    .isEqualTo("The request could not be verified. Please reload and try again.");
        });
        // Nothing was changed: Ana is still logged in.
        assertThat(api.send("GET", "/api/session", null, ana.cookie(), null).status()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ logs

    @Test
    void theLogsShowNoPasswordHashOrSessionIdentifier(CapturedOutput output) {
        LoggedIn ana = api.logInAs(ANA, ANA_PASSWORD);
        api.logIn(ANA, "wrong-password");
        api.logIn("nobody@example.com", "another-wrong-password");
        api.send("GET", "/api/session", null, ana.cookie(), null);
        api.send("DELETE", "/api/session", null, ana.cookie(), ana.csrfToken());

        String anaHash = jdbc.sql("select password_hash from users where email = ?").param(ANA)
                .query(String.class).single();

        // The cookie carries the session identifier in Base64.
        String sessionId = new String(java.util.Base64.getDecoder().decode(ana.cookie()));
        assertThat(sessionId).matches("[0-9a-f-]{36}");

        assertThat(output.getAll()).doesNotContain(                                       // SEC05, SEC10
                ANA_PASSWORD, "wrong-password", "another-wrong-password", anaHash, "{bcrypt}",
                ana.cookie(), sessionId, ana.csrfToken());
    }

    private static Map<String, List<String>> headersWithoutDate(HttpHeaders headers) {
        Map<String, List<String>> map = new TreeMap<>(headers.map());
        map.remove("date");
        return map;
    }
}
