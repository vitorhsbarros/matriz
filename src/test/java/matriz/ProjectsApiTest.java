package matriz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import matriz.ApiClient.LoggedIn;
import matriz.ApiClient.Response;
import matriz.TestUsers.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;

/**
 * Increment I2: create, get, edit and delete a project (US03, US14, US15, US04 AC02), through real HTTP
 * requests. Each test creates its own users; the demo data is used only where a story names it
 * (Website: Ana manager, Rui member, Sara not a member).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import(TestcontainersConfiguration.class)
class ProjectsApiTest {

    static final String WEBSITE = "5b0c6f0e-2f4a-4c47-9f39-1b2f7f6f9a10";

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PasswordEncoder encoder;

    ApiClient api;
    ProjectFixtures fixtures;
    TestUser owner;
    LoggedIn ownerSession;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
        fixtures = new ProjectFixtures(api, jdbc);
        owner = TestUsers.create(jdbc, encoder, "Marta Reis");
        ownerSession = api.logInAs(owner.email(), TestUsers.PASSWORD);
    }

    // ------------------------------------------------------------------ US03 — create

    @Test
    void createAProjectWithNameAndDescription() {
        Response created = create(Map.of("name", "Website", "description", "New company website"));   // AC01

        assertThat(created.status()).isEqualTo(201);
        JsonNode project = created.json();
        assertThat(project.get("name").asString()).isEqualTo("Website");
        assertThat(project.get("description").asString()).isEqualTo("New company website");
        assertThat(project.get("role").asString()).isEqualTo("MANAGER");
        assertThat(created.location()).isEqualTo("/api/projects/" + project.get("id").asString());

        UUID id = UUID.fromString(project.get("id").asString());
        UUID manager = jdbc.sql("select manager_id from project where id = ?").param(id).query(UUID.class).single();
        assertThat(manager).isEqualTo(owner.id());                                   // D07: manager...
        assertThat(fixtures.count("project_member", "project_id", id)).isEqualTo(1);  // ...and member
        assertThat(fixtures.count("task", "project_id", id)).isZero();
        assertThat(dashboardNames(ownerSession)).containsExactly("Website");
    }

    @Test
    void createAProjectWithoutADescription() {
        Response noDescription = create(Map.of("name", "Website"));                                      // AC02
        Response blankDescription = create(Map.of("name", "Website", "description", "   "));               // D01
        Map<String, Object> nullDescription = new HashMap<>();
        nullDescription.put("name", "Website");
        nullDescription.put("description", null);

        assertThat(noDescription.status()).isEqualTo(201);
        assertThat(noDescription.json().get("description").isNull()).isTrue();
        assertThat(blankDescription.json().get("description").isNull()).isTrue();
        assertThat(create(nullDescription).status()).isEqualTo(201);
    }

    @Test
    void theNameIsRequired() {
        for (String body : List.of("{\"name\":\"\"}", "{\"name\":\"   \"}", "{}", "{\"name\":null}")) {     // AC03
            Response response = api.send("POST", "/api/projects", body, ownerSession.cookie(),
                    ownerSession.csrfToken());

            assertThat(response.status()).as(body).isEqualTo(422);
            assertThat(response.field("detail")).as(body).isEqualTo("Project name is required.");
        }
        assertThat(fixtures.count("project", "manager_id", owner.id())).isZero();
    }

    @Test
    void nameLimits() {
        assertThat(create(Map.of("name", "W")).status()).isEqualTo(201);                                 // AC04
        assertThat(create(Map.of("name", "W".repeat(100))).status()).isEqualTo(201);
        assertThat(create(Map.of("name", "  " + "W".repeat(100) + "  ")).status()).isEqualTo(201);        // D02

        Response tooLong = create(Map.of("name", "W".repeat(101)));
        assertThat(tooLong.status()).isEqualTo(422);
        assertThat(tooLong.field("detail")).isEqualTo("Project name must be at most 100 characters.");
    }

    @Test
    void descriptionLimits() {
        assertThat(create(Map.of("name", "Website", "description", "d".repeat(2000))).status()).isEqualTo(201); // AC05

        Response tooLong = create(Map.of("name", "Website", "description", "d".repeat(2001)));
        assertThat(tooLong.status()).isEqualTo(422);
        assertThat(tooLong.field("detail")).isEqualTo("Description must be at most 2000 characters.");
    }

    @Test
    void theNameIsCheckedBeforeTheDescription() {
        Response both = create(Map.of("name", "", "description", "d".repeat(2001)));                     // AS08

        assertThat(both.field("detail")).isEqualTo("Project name is required.");
    }

    @Test
    void repeatedNamesAreAllowed() {
        String first = create(Map.of("name", "Website")).json().get("id").asString();                    // AC06
        String second = create(Map.of("name", "Website")).json().get("id").asString();

        assertThat(second).isNotEqualTo(first);
        assertThat(dashboardNames(ownerSession)).containsExactly("Website", "Website");
    }

    @Test
    void theManagerIsAlwaysTheLoggedInUser() {
        // Neither a body field nor a query parameter can name someone else as the manager (NFR03).
        Response withManagerField = api.send("POST", "/api/projects",
                "{\"name\":\"Website\",\"managerId\":\"" + UUID.randomUUID() + "\"}",
                ownerSession.cookie(), ownerSession.csrfToken());
        Response withQuery = api.send("POST", "/api/projects?userId=" + UUID.randomUUID(),
                "{\"name\":\"Website\"}", ownerSession.cookie(), ownerSession.csrfToken());

        assertThat(withManagerField.status()).isEqualTo(400);                                            // DoD07
        assertThat(withQuery.status()).isEqualTo(201);
        assertThat(withQuery.json().get("role").asString()).isEqualTo("MANAGER");
        UUID id = UUID.fromString(withQuery.json().get("id").asString());
        assertThat(jdbc.sql("select manager_id from project where id = ?").param(id).query(UUID.class).single())
                .isEqualTo(owner.id());
    }

    @Test
    void createIsRefusedWithoutASessionWithoutTheTokenOrWithAMalformedBody() {
        Response noSession = api.send("POST", "/api/projects", "{\"name\":\"Website\"}", null, null);
        Response noToken = api.send("POST", "/api/projects", "{\"name\":\"Website\"}", ownerSession.cookie(), null);
        Response malformed = api.send("POST", "/api/projects", "{\"name\":", ownerSession.cookie(),
                ownerSession.csrfToken());
        Response wrongType = api.send("POST", "/api/projects", "{\"name\":42}", ownerSession.cookie(),
                ownerSession.csrfToken());

        assertThat(noSession.status()).isEqualTo(401);                                                   // DoD02
        assertThat(noToken.status()).isEqualTo(403);                                                     // DoD06
        assertThat(noToken.field("detail")).isEqualTo("The request could not be verified. Please reload and try again.");
        assertThat(malformed.status()).isEqualTo(400);                                                   // DoD07
        assertThat(malformed.field("detail")).isEqualTo("The request body is not valid.");
        assertThat(wrongType.status()).isEqualTo(400);
        assertThat(fixtures.count("project", "manager_id", owner.id())).isZero();
    }

    // ------------------------------------------------------------------ get (US04 AC02) and access

    @Test
    void membersGetTheProjectWithTheirRole() {
        LoggedIn ana = api.logInAs("ana@example.com", "test-password-a");
        LoggedIn rui = api.logInAs("rui@example.com", "test-password-r");

        Response asManager = api.send("GET", "/api/projects/" + WEBSITE, null, ana.cookie(), null);
        Response asMember = api.send("GET", "/api/projects/" + WEBSITE, null, rui.cookie(), null);

        assertThat(asManager.status()).isEqualTo(200);
        assertThat(asManager.json().get("name").asString()).isEqualTo("Website");
        assertThat(asManager.json().get("role").asString()).isEqualTo("MANAGER");
        assertThat(asMember.json().get("role").asString()).isEqualTo("MEMBER");
    }

    @Test
    void aNonMemberGetsProjectNotFoundForEveryOperation() {
        LoggedIn sara = api.logInAs("sara@example.com", "test-password-s");

        List<Response> responses = List.of(                                                              // DoD03
                api.send("GET", "/api/projects/" + WEBSITE, null, sara.cookie(), null),
                api.send("PATCH", "/api/projects/" + WEBSITE, "{\"name\":\"Mine\"}", sara.cookie(), sara.csrfToken()),
                api.send("DELETE", "/api/projects/" + WEBSITE, null, sara.cookie(), sara.csrfToken()),
                api.send("GET", "/api/projects/" + UUID.randomUUID(), null, sara.cookie(), null),
                api.send("GET", "/api/projects/not-an-identifier", null, sara.cookie(), null));

        assertThat(responses).allSatisfy(response -> {
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.field("detail")).isEqualTo("Project not found.");
        });
        assertThat(jdbc.sql("select name from project where id = ?").param(UUID.fromString(WEBSITE))
                .query(String.class).single()).isEqualTo("Website");
    }

    @Test
    void getIsRefusedWithoutASession() {
        Response response = api.send("GET", "/api/projects/" + WEBSITE, null, null, null);

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.field("detail")).isEqualTo("Please log in to continue.");
    }

    // ------------------------------------------------------------------ US14 — edit

    @Test
    void editTheNameAndDescription() {
        UUID project = fixtures.createProject(ownerSession, "Website");
        TestUser member = TestUsers.create(jdbc, encoder, "Rita Melo");
        fixtures.addMember(project, member.id());

        Response edited = patch(project, Map.of("name", "Company website", "description", "Public site and blog"));

        assertThat(edited.status()).isEqualTo(200);                                                      // AC01
        assertThat(edited.json().get("name").asString()).isEqualTo("Company website");
        assertThat(edited.json().get("description").asString()).isEqualTo("Public site and blog");
        assertThat(dashboardNames(api.logInAs(member.email(), TestUsers.PASSWORD))).containsExactly("Company website");
    }

    @Test
    void clearTheDescription() {
        UUID project = projectWithDescription();
        Map<String, Object> clear = new HashMap<>();
        clear.put("description", null);

        Response cleared = patch(project, clear);                                                        // AC02

        assertThat(cleared.status()).isEqualTo(200);
        assertThat(cleared.json().get("description").isNull()).isTrue();
        assertThat(cleared.json().get("name").asString()).isEqualTo("Website");
        assertThat(patch(projectWithDescription(), Map.of("description", "")).json().get("description").isNull()).isTrue();
    }

    @Test
    void aClearedNameIsRefusedAndNothingChanges() {
        UUID project = projectWithDescription();

        for (String body : List.of("{\"name\":\"\"}", "{\"name\":null}", "{\"name\":\"  \",\"description\":\"x\"}")) {
            Response response = api.send("PATCH", "/api/projects/" + project, body, ownerSession.cookie(),
                    ownerSession.csrfToken());

            assertThat(response.status()).as(body).isEqualTo(422);                                        // AC03
            assertThat(response.field("detail")).as(body).isEqualTo("Project name is required.");
        }
        JsonNode unchanged = get(project).json();
        assertThat(unchanged.get("name").asString()).isEqualTo("Website");
        assertThat(unchanged.get("description").asString()).isEqualTo("New company website");
    }

    @Test
    void aMemberWhoIsntTheManagerCantEdit() {
        LoggedIn rui = api.logInAs("rui@example.com", "test-password-r");

        Response response = api.send("PATCH", "/api/projects/" + WEBSITE, "{\"name\":\"Rui's site\"}",
                rui.cookie(), rui.csrfToken());

        assertThat(response.status()).isEqualTo(403);                                                    // AC04
        assertThat(response.field("detail")).isEqualTo("Only the project manager can edit the project.");
        assertThat(jdbc.sql("select name from project where id = ?").param(UUID.fromString(WEBSITE))
                .query(String.class).single()).isEqualTo("Website");
    }

    @Test
    void editNameLimits() {
        UUID project = fixtures.createProject(ownerSession, "Website");

        assertThat(patch(project, Map.of("name", "W".repeat(100))).status()).isEqualTo(200);             // AC06
        Response tooLong = patch(project, Map.of("name", "W".repeat(101)));
        assertThat(tooLong.status()).isEqualTo(422);
        assertThat(tooLong.field("detail")).isEqualTo("Project name must be at most 100 characters.");
        assertThat(patch(project, Map.of("description", "d".repeat(2001))).field("detail"))
                .isEqualTo("Description must be at most 2000 characters.");
    }

    @Test
    void saveWithoutChanges() {
        UUID project = projectWithDescription();

        Response same = patch(project, Map.of("name", "Website", "description", "New company website"));  // AC07
        Response empty = api.send("PATCH", "/api/projects/" + project, "{}", ownerSession.cookie(),
                ownerSession.csrfToken());

        assertThat(same.status()).isEqualTo(200);
        assertThat(empty.status()).isEqualTo(200);
        assertThat(empty.json().get("name").asString()).isEqualTo("Website");
        assertThat(empty.json().get("description").asString()).isEqualTo("New company website");
    }

    @Test
    void aNameUsedByAnotherProjectIsAllowed() {
        fixtures.createProject(ownerSession, "App");
        UUID project = fixtures.createProject(ownerSession, "Website");

        Response renamed = patch(project, Map.of("name", "App"));                                        // AC08, D06

        assertThat(renamed.status()).isEqualTo(200);
        assertThat(dashboardNames(ownerSession)).containsExactly("App", "App");
    }

    @Test
    void editIsRefusedWithoutTheTokenOrWithAMalformedBody() {
        UUID project = fixtures.createProject(ownerSession, "Website");

        Response noToken = api.send("PATCH", "/api/projects/" + project, "{\"name\":\"X\"}", ownerSession.cookie(), null);
        Response unknownField = api.send("PATCH", "/api/projects/" + project, "{\"managerId\":\"x\"}",
                ownerSession.cookie(), ownerSession.csrfToken());
        Response noSession = api.send("PATCH", "/api/projects/" + project, "{\"name\":\"X\"}", null, null);

        assertThat(noToken.status()).isEqualTo(403);                                                     // DoD06
        assertThat(unknownField.status()).isEqualTo(400);                                                // DoD07
        assertThat(noSession.status()).isEqualTo(401);                                                   // DoD02
        assertThat(get(project).json().get("name").asString()).isEqualTo("Website");
    }

    // ------------------------------------------------------------------ US15 — delete

    @Test
    void deletingAProjectDeletesEverythingThatBelongsToIt() {
        UUID project = fixtures.createProject(ownerSession, "Website");
        TestUser member = TestUsers.create(jdbc, encoder, "Rita Melo");
        fixtures.addMember(project, member.id());
        UUID testTheSite = fixtures.addTask(ownerSession, project, "Test the site", "TODO");
        UUID createLogo = fixtures.addTask(ownerSession, project, "Create logo", "IN_PROGRESS");
        fixtures.assignUser(testTheSite, member.id());
        UUID design = fixtures.assignNewTeam(createLogo, member.id());
        fixtures.addComment(createLogo, member.id(), "Use the new colours");

        Response deleted = api.send("DELETE", "/api/projects/" + project, null, ownerSession.cookie(),
                ownerSession.csrfToken());

        assertThat(deleted.status()).isEqualTo(204);                                                     // AC02
        assertThat(fixtures.count("project", "id", project)).isZero();
        assertThat(fixtures.count("project_member", "project_id", project)).isZero();
        assertThat(fixtures.count("task", "project_id", project)).isZero();
        assertThat(fixtures.count("task_assignee_user", "task_id", testTheSite)).isZero();
        assertThat(fixtures.count("task_assignee_team", "task_id", createLogo)).isZero();
        assertThat(fixtures.count("task_comment", "task_id", createLogo)).isZero();
        assertThat(fixtures.count("team", "id", design)).isEqualTo(1);                                  // AC06
        assertThat(fixtures.count("team_member", "team_id", design)).isEqualTo(1);
        assertThat(dashboardNames(ownerSession)).isEmpty();
        assertThat(dashboardNames(api.logInAs(member.email(), TestUsers.PASSWORD))).isEmpty();
        Response afterwards = get(project);
        assertThat(afterwards.status()).isEqualTo(404);
        assertThat(afterwards.field("detail")).isEqualTo("Project not found.");
    }

    @Test
    void aMemberWhoIsntTheManagerCantDelete() {
        LoggedIn rui = api.logInAs("rui@example.com", "test-password-r");

        Response response = api.send("DELETE", "/api/projects/" + WEBSITE, null, rui.cookie(), rui.csrfToken());

        assertThat(response.status()).isEqualTo(403);                                                    // AC04
        assertThat(response.field("detail")).isEqualTo("Only the project manager can delete the project.");
        assertThat(fixtures.count("project", "id", UUID.fromString(WEBSITE))).isEqualTo(1);
        assertThat(fixtures.count("project_member", "project_id", UUID.fromString(WEBSITE))).isEqualTo(2);
    }

    @Test
    void deleteAProjectWithoutTasks() {
        UUID project = fixtures.createProject(ownerSession, "Intranet");

        Response deleted = api.send("DELETE", "/api/projects/" + project, null, ownerSession.cookie(),
                ownerSession.csrfToken());                                                               // AC07

        assertThat(deleted.status()).isEqualTo(204);
        assertThat(fixtures.count("project", "id", project)).isZero();
    }

    @Test
    void deleteIsRefusedWithoutTheToken() {
        UUID project = fixtures.createProject(ownerSession, "Website");

        Response noToken = api.send("DELETE", "/api/projects/" + project, null, ownerSession.cookie(), null);

        assertThat(noToken.status()).isEqualTo(403);                                                     // DoD06
        assertThat(fixtures.count("project", "id", project)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ helpers

    private UUID projectWithDescription() {
        Response created = create(Map.of("name", "Website", "description", "New company website"));
        return UUID.fromString(created.json().get("id").asString());
    }

    private Response create(Map<String, ?> body) {
        return api.send("POST", "/api/projects", ApiClient.json(body), ownerSession.cookie(), ownerSession.csrfToken());
    }

    private Response patch(UUID project, Map<String, ?> body) {
        return api.send("PATCH", "/api/projects/" + project, ApiClient.json(body), ownerSession.cookie(),
                ownerSession.csrfToken());
    }

    private Response get(UUID project) {
        return api.send("GET", "/api/projects/" + project, null, ownerSession.cookie(), null);
    }

    private List<String> dashboardNames(LoggedIn user) {
        JsonNode projects = api.send("GET", "/api/projects", null, user.cookie(), null).json().get("projects");
        return projects.valueStream().map(project -> project.get("name").asString()).toList();
    }
}
