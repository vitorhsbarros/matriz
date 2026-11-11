package matriz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import matriz.ApiClient.LoggedIn;
import matriz.ApiClient.Response;
import matriz.projects.ManagerOnlyAction;
import matriz.projects.ProjectAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The project access check (NFR03, SEC02, SEC03, DoD03) with different users, through direct requests to the
 * API. The real project operations arrive in I2, I4 and I6 (implementation-plan.md), so the check is called
 * here from an endpoint that exists only in this test.
 *
 * <p>Demo data: in <em>Website</em>, Ana is the manager, Rui a member and Sara not a member; in <em>App</em>,
 * Rui is the manager and Ana a member.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import({TestcontainersConfiguration.class, ProjectAccessTest.ProbeConfiguration.class})
class ProjectAccessTest {

    static final String WEBSITE = "5b0c6f0e-2f4a-4c47-9f39-1b2f7f6f9a10";
    static final String APP = "9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d";

    /** Test-only operations: reading needs a member, editing needs the manager. */
    @RestController
    static class ProbeController {

        private final ProjectAccess access;

        ProbeController(ProjectAccess access) {
            this.access = access;
        }

        @GetMapping("/api/test-probe/projects/{projectId}")
        String read(@PathVariable UUID projectId, Authentication authentication) {
            access.requireMember(projectId, UUID.fromString(authentication.getName()));
            return "{\"read\":\"ok\"}";
        }

        @PatchMapping("/api/test-probe/projects/{projectId}")
        String edit(@PathVariable UUID projectId, Authentication authentication) {
            access.requireManager(projectId, UUID.fromString(authentication.getName()),
                    ManagerOnlyAction.EDIT_PROJECT);
            return "{\"edited\":\"ok\"}";
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {

        @Bean
        ProbeController probeController(ProjectAccess access) {
            return new ProbeController(access);
        }
    }

    @LocalServerPort
    int port;

    ApiClient api;
    LoggedIn ana;
    LoggedIn rui;
    LoggedIn sara;

    @BeforeEach
    void logIn() {
        api = new ApiClient(port);
        ana = api.logInAs("ana@example.com", "test-password-a");
        rui = api.logInAs("rui@example.com", "test-password-r");
        sara = api.logInAs("sara@example.com", "test-password-s");
    }

    @Test
    void membersCanReadTheirProject() {
        assertThat(read(ana, WEBSITE).status()).isEqualTo(200);   // manager
        assertThat(read(rui, WEBSITE).status()).isEqualTo(200);   // member
        assertThat(read(ana, APP).status()).isEqualTo(200);       // the roles are per project
    }

    @Test
    void aNonMemberGetsTheSameAnswerAsForAProjectThatDoesntExist() {
        Response notMember = read(sara, WEBSITE);
        Response noSuchProject = read(sara, UUID.randomUUID().toString());

        assertThat(notMember.status()).isEqualTo(404);                                     // DoD03
        assertThat(notMember.field("detail")).isEqualTo("Project not found.");
        // SEC03: the same answer; only "instance", the requested path itself, differs.
        assertThat(withoutInstance(notMember.body())).isEqualTo(withoutInstance(noSuchProject.body()));
        assertThat(edit(sara, WEBSITE).status()).isEqualTo(404);
        assertThat(edit(sara, WEBSITE).body()).doesNotContain("manager");
    }

    @Test
    void onlyTheManagerCanTakeAManagerOnlyAction() {
        Response member = edit(rui, WEBSITE);

        assertThat(member.status()).isEqualTo(403);                                        // NFR03, US14 AC04
        assertThat(member.field("detail")).isEqualTo("Only the project manager can edit the project.");
        assertThat(edit(ana, WEBSITE).status()).isEqualTo(200);
        assertThat(edit(rui, APP).status()).isEqualTo(200);
        assertThat(edit(ana, APP).status()).isEqualTo(403);
    }

    @Test
    void theUserIsTheOneOfTheSessionWhateverTheRequestSays() {
        // Sara sends Ana's identifier as if it were proof of identity: it is ignored.
        Response response = api.send("GET", "/api/test-probe/projects/" + WEBSITE + "?userId=ana", null,
                sara.cookie(), null);

        assertThat(response.status()).isEqualTo(404);
    }

    @Test
    void theCheckAlsoNeedsALoggedInUserAndTheCsrfToken() {
        Response noSession = api.send("GET", "/api/test-probe/projects/" + WEBSITE, null, null, null);
        Response noToken = api.send("PATCH", "/api/test-probe/projects/" + WEBSITE, null, ana.cookie(), null);

        assertThat(noSession.status()).isEqualTo(401);
        assertThat(noToken.status()).isEqualTo(403);
        assertThat(noToken.field("detail"))
                .isEqualTo("The request could not be verified. Please reload and try again.");
    }

    private static String withoutInstance(String body) {
        return body.replaceAll("\"instance\":\"[^\"]*\",?", "");
    }

    private Response read(LoggedIn user, String projectId) {
        return api.send("GET", "/api/test-probe/projects/" + projectId, null, user.cookie(), null);
    }

    private Response edit(LoggedIn user, String projectId) {
        return api.send("PATCH", "/api/test-probe/projects/" + projectId, null, user.cookie(), user.csrfToken());
    }
}
