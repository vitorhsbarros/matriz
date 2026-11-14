package matriz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import matriz.ApiClient.LoggedIn;
import matriz.ApiClient.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Access between the three demo accounts, as the seed creates them: Ana manages "Website" and is a member of
 * "App", Rui manages "App" and is a member of "Website", and Sara is a member of neither. Each attempt to read or
 * change something without permission must be refused and leave the data as it was.
 *
 * <p>The passwords are fictitious and exist only in the tests. Whatever a test creates is deleted afterwards, so
 * the demo projects stay as the other tests expect them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import(TestcontainersConfiguration.class)
class DemoAccountsAuthorizationTest {

    static final String WEBSITE = "/api/projects/5b0c6f0e-2f4a-4c47-9f39-1b2f7f6f9a10";
    static final String APP = "/api/projects/9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d";

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    ApiClient api;
    LoggedIn ana;
    LoggedIn rui;
    LoggedIn sara;
    String task;
    String comment;
    final List<Runnable> cleanUp = new ArrayList<>();

    @BeforeEach
    void logInAndAddATaskWithAComment() {
        api = new ApiClient(port);
        ana = api.logInAs("ana@example.com", "test-password-a");
        rui = api.logInAs("rui@example.com", "test-password-r");
        sara = api.logInAs("sara@example.com", "test-password-s");
        task = WEBSITE + "/tasks/" + created(ana, WEBSITE + "/tasks", Map.of("title", "Review the home page"));
        cleanUp.add(() -> api.send("DELETE", task, null, ana.cookie(), ana.csrfToken()));
        comment = task + "/comments/" + created(ana, task + "/comments", Map.of("text", "Ana's comment"));
    }

    @AfterEach
    void deleteWhatTheTestCreated() {
        cleanUp.reversed().forEach(Runnable::run);
    }

    @Test
    void someoneWhoIsNotAMemberCanNeitherReadNorChangeTheProjectOrItsTasks() {
        Map<String, String> before = websiteState();

        List<Response> answers = List.of(
                send(sara, "GET", WEBSITE, null),
                send(sara, "PATCH", WEBSITE, Map.of("name", "Sara's project")),
                send(sara, "DELETE", WEBSITE, null),
                send(sara, "GET", WEBSITE + "/members", null),
                send(sara, "POST", WEBSITE + "/members", Map.of("email", "sara@example.com")),
                send(sara, "GET", WEBSITE + "/tasks", null),
                send(sara, "POST", WEBSITE + "/tasks", Map.of("title", "Sara's task")),
                send(sara, "GET", task, null),
                send(sara, "PATCH", task, Map.of("title", "Changed by Sara")),
                send(sara, "DELETE", task, null),
                send(sara, "GET", task + "/comments", null),
                send(sara, "POST", task + "/comments", Map.of("text", "Sara's comment")),
                send(sara, "PATCH", comment, Map.of("text", "Changed by Sara")),
                send(sara, "DELETE", comment, null));

        assertThat(answers).allSatisfy(answer -> {
            assertThat(answer.status()).isEqualTo(404);
            assertThat(answer.field("detail")).isEqualTo("Project not found.");
            assertThat(answer.body()).doesNotContain("Review the home page").doesNotContain("Ana's comment");
        });
        assertThat(websiteState()).isEqualTo(before);
    }

    @Test
    void aMemberWhoIsNotTheManagerCannotDoTheManagersActions() {
        Map<String, String> before = websiteState();

        assertRefused(send(rui, "PATCH", WEBSITE, Map.of("name", "Rui's website")), 403,
                "Only the project manager can edit the project.");
        assertRefused(send(rui, "DELETE", WEBSITE, null), 403, "Only the project manager can delete the project.");
        assertRefused(send(rui, "POST", WEBSITE + "/members", Map.of("email", "sara@example.com")), 403,
                "Only the project manager can manage members.");

        assertThat(websiteState()).isEqualTo(before);
    }

    @Test
    void aTaskOfAnotherProjectCannotBeReachedThroughTheAddressOfOneOfYourOwn() {
        String anaOnly = "/api/projects/" + created(ana, "/api/projects", Map.of("name", "Ana's notes"));
        cleanUp.add(() -> api.send("DELETE", anaOnly, null, ana.cookie(), ana.csrfToken()));
        String hidden = created(ana, anaOnly + "/tasks", Map.of("title", "Private task"));
        String throughApp = APP + "/tasks/" + hidden;

        for (Response answer : List.of(
                send(rui, "GET", throughApp, null),
                send(rui, "PATCH", throughApp, Map.of("title", "Changed by Rui")),
                send(rui, "DELETE", throughApp, null),
                send(rui, "POST", throughApp + "/comments", Map.of("text", "Rui's comment")))) {
            assertRefused(answer, 404, "Task not found.");
        }
        assertThat(send(ana, "GET", anaOnly + "/tasks/" + hidden, null).json().get("title").asString())
                .isEqualTo("Private task");
    }

    @Test
    void onlyTheAuthorCanChangeAComment() {
        assertRefused(send(rui, "PATCH", comment, Map.of("text", "Changed by Rui")), 403,
                "Only the author can change this comment.");
        assertRefused(send(rui, "DELETE", comment, null), 403, "Only the author can change this comment.");

        assertThat(send(ana, "GET", task + "/comments", null).json().get(0).get("text").asString())
                .isEqualTo("Ana's comment");
    }

    @Test
    void onlyTheManagerCanChangeATeam() {
        String team = "/api/teams/" + created(ana, "/api/teams", Map.of("name", "Reviewers " + UUID.randomUUID()));
        cleanUp.add(() -> api.send("DELETE", team, null, ana.cookie(), ana.csrfToken()));

        assertRefused(send(rui, "PATCH", team, Map.of("name", "Rui's team")), 403,
                "Only the team manager can edit the team.");
        assertRefused(send(rui, "DELETE", team, null), 403, "Only the team manager can delete the team.");
        assertRefused(send(rui, "POST", team + "/members", Map.of("email", "rui@example.com")), 403,
                "Only the team manager can manage members.");
    }

    @Test
    void anotherUsersVerificationTokenDoesNotAuthorizeAChange() {
        Response answer = api.send("POST", WEBSITE + "/tasks", ApiClient.json(Map.of("title", "Sent with Rui's token")),
                ana.cookie(), rui.csrfToken());

        assertRefused(answer, 403, "The request could not be verified. Please reload and try again.");
        assertThat(websiteTaskTitles()).doesNotContain("Sent with Rui's token");
    }

    private Response send(LoggedIn user, String method, String path, Map<String, String> body) {
        return api.send(method, path, body == null ? null : ApiClient.json(body), user.cookie(),
                "GET".equals(method) ? null : user.csrfToken());
    }

    private String created(LoggedIn user, String path, Map<String, String> body) {
        Response answer = send(user, "POST", path, body);
        assertThat(answer.status()).as("POST " + path).isEqualTo(201);
        return answer.json().get("id").asString();
    }

    private static void assertRefused(Response answer, int status, String message) {
        assertThat(answer.status()).isEqualTo(status);
        assertThat(answer.field("detail")).isEqualTo(message);
    }

    /** What a refused change could have altered in "Website": its name, members, tasks and the comment. */
    private Map<String, String> websiteState() {
        return Map.of(
                "project", send(ana, "GET", WEBSITE, null).body(),
                "members", send(ana, "GET", WEBSITE + "/members", null).body(),
                "tasks", String.join("|", websiteTaskTitles()),
                "comments", send(ana, "GET", task + "/comments", null).body());
    }

    private List<String> websiteTaskTitles() {
        List<String> titles = new ArrayList<>();
        send(ana, "GET", WEBSITE + "/tasks", null).json().forEach(t -> titles.add(t.get("title").asString()));
        return titles;
    }
}
