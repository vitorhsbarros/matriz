package matriz;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Increments I4 and I5: a task's assignees, people and teams (US10, US13; D08, D09, D10), and who can be
 * assigned (listEligibleAssignees), through real HTTP requests. Ana, Rui and Sara are members of the project;
 * Marta isn't.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import(TestcontainersConfiguration.class)
class AssigneesApiTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PasswordEncoder encoder;

    ApiClient api;
    ProjectFixtures fixtures;
    TestUser anaUser;
    TestUser ruiUser;
    TestUser saraUser;
    TestUser martaUser;
    LoggedIn ana;
    LoggedIn rui;
    LoggedIn marta;
    UUID website;
    UUID testTheSite;
    String suffix;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
        fixtures = new ProjectFixtures(api, jdbc);
        suffix = UUID.randomUUID().toString().substring(0, 8);
        anaUser = TestUsers.create(jdbc, encoder, "Ana Silva");
        ruiUser = TestUsers.create(jdbc, encoder, "Rui Costa");
        saraUser = TestUsers.create(jdbc, encoder, "Sara Lopes");
        martaUser = TestUsers.create(jdbc, encoder, "Marta Reis");
        ana = api.logInAs(anaUser.email(), TestUsers.PASSWORD);
        rui = api.logInAs(ruiUser.email(), TestUsers.PASSWORD);
        marta = api.logInAs(martaUser.email(), TestUsers.PASSWORD);
        website = fixtures.createProject(ana, "Website");
        fixtures.addMember(ana, website, ruiUser.email());
        fixtures.addMember(ana, website, saraUser.email());
        testTheSite = fixtures.addTask(ana, website, "Test the site", "TODO");
    }

    // ------------------------------------------------------------------ US10 — people

    @Test
    void assignAMember() {
        Response response = assign(ana, "person", ruiUser.id());                                        // AC01

        assertThat(response.status()).isEqualTo(200);
        JsonNode assignee = response.json().get("assignees").get(0);
        assertThat(assignee.get("type").asString()).isEqualTo("person");
        assertThat(assignee.get("userId").asString()).isEqualTo(ruiUser.id().toString());
        assertThat(assignee.get("name").asString()).isEqualTo("Rui Costa");
        assertThat(assignee.has("teamId")).isFalse();
        assertThat(boardAssignees()).containsExactly("Rui Costa");
    }

    @Test
    void assignSeveralMembersAndRemoveThem() {
        assign(ana, "person", saraUser.id());
        assign(rui, "person", ruiUser.id());                                                             // D08

        assertThat(names(task())).containsExactly("Rui Costa", "Sara Lopes");                              // AC02

        assertThat(api.send("DELETE", path() + "/assignees/users/" + ruiUser.id(), null, ana.cookie(), ana.csrfToken())
                .status()).isEqualTo(200);                                                               // AC03
        assertThat(names(task())).containsExactly("Sara Lopes");
        JsonNode last = api.send("DELETE", path() + "/assignees/users/" + saraUser.id(), null, ana.cookie(),
                ana.csrfToken()).json();                                                                 // AC10
        assertThat(last.get("assignees").isEmpty()).isTrue();
    }

    @Test
    void aPersonWhoIsntAMemberCantBeAssigned() {
        for (String id : List.of(martaUser.id().toString(), UUID.randomUUID().toString(), "not-an-identifier")) { // AC04
            Response response = api.send("POST", path() + "/assignees",
                    ApiClient.json(Map.of("type", "person", "userId", id)), ana.cookie(), ana.csrfToken());
            assertThat(response.status()).as(id).isEqualTo(422);
            assertThat(response.field("detail")).isEqualTo("The assignee must be a member of the project.");
        }
        assertThat(names(task())).isEmpty();
    }

    @Test
    void aMemberRemovedInTheMeantimeCantBeAssigned() {
        api.send("DELETE", "/api/projects/" + website + "/members/" + ruiUser.id(), null, ana.cookie(), ana.csrfToken());

        Response response = assign(ana, "person", ruiUser.id());                                        // AC05

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.field("detail")).isEqualTo("The assignee must be a member of the project.");
    }

    @Test
    void aNonMemberCantChangeTheAssignees() {
        Response adding = assign(marta, "person", ruiUser.id());                                        // AC06, DoD03
        Response removing = api.send("DELETE", path() + "/assignees/users/" + ruiUser.id(), null, marta.cookie(),
                marta.csrfToken());

        assertThat(List.of(adding, removing)).allSatisfy(response -> {
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.field("detail")).isEqualTo("Project not found.");
        });
    }

    @Test
    void assigningAndRemovingTwiceChangeNothingElse() {
        JsonNode before = task();
        assign(ana, "person", ruiUser.id());
        Response again = assign(ana, "person", ruiUser.id());                                           // AC07

        assertThat(again.status()).isEqualTo(200);
        assertThat(names(again.json())).containsExactly("Rui Costa");
        Response notAssigned = api.send("DELETE", path() + "/assignees/users/" + saraUser.id(), null, ana.cookie(),
                ana.csrfToken());                                                                        // AC08
        assertThat(notAssigned.status()).isEqualTo(200);
        assertThat(names(notAssigned.json())).containsExactly("Rui Costa");
        for (String field : List.of("title", "description", "dueDate", "priority", "status")) {         // AC09
            assertThat(again.json().get(field)).as(field).isEqualTo(before.get(field));
        }
    }

    // ------------------------------------------------------------------ US13 — teams

    @Test
    void assignATeamNextToAPerson() {
        UUID design = team("Design", rui, saraUser.email());
        assign(ana, "person", anaUser.id());

        Response response = assign(ana, "team", design);                                                // AC01, AC02

        assertThat(response.status()).isEqualTo(200);
        assertThat(names(response.json())).containsExactly("Ana Silva", "Design " + suffix);             // AS21
        JsonNode team = response.json().get("assignees").get(1);
        assertThat(team.get("type").asString()).isEqualTo("team");
        assertThat(team.get("teamId").asString()).isEqualTo(design.toString());
        assertThat(team.has("userId")).isFalse();

        assertThat(api.send("DELETE", path() + "/assignees/teams/" + design, null, ana.cookie(), ana.csrfToken())
                .json().get("assignees").size()).isEqualTo(1);                                          // AC03
        assertThat(names(task())).containsExactly("Ana Silva");
    }

    @Test
    void aTeamWithMembersOutsideTheProjectCantBeAssigned() {
        UUID marketing = team("Marketing", rui, martaUser.email());

        Response response = assign(ana, "team", marketing);                                             // AC04

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.field("detail")).isEqualTo("All team members must belong to the project.");
        assertThat(names(task())).isEmpty();
    }

    @Test
    void aTeamThatDoesntExistCantBeAssigned() {
        for (String id : List.of(UUID.randomUUID().toString(), "not-an-identifier")) {                   // AC05, AS10
            Response response = api.send("POST", path() + "/assignees",
                    ApiClient.json(Map.of("type", "team", "teamId", id)), ana.cookie(), ana.csrfToken());
            assertThat(response.status()).isEqualTo(422);
            assertThat(response.field("detail")).isEqualTo("Team not found.");
        }
    }

    @Test
    void teamEdgeCases() {
        UUID qa = fixtures.createTeam(ana, "QA " + suffix);                                              // only Ana
        assertThat(assign(ana, "team", qa).status()).isEqualTo(200);                                     // AC06
        assertThat(assign(ana, "team", qa).json().get("assignees").size()).isEqualTo(1);                 // AC09

        UUID design = team("Design", rui, saraUser.email());
        assign(ana, "team", design);
        api.send("POST", "/api/teams/" + design + "/members", ApiClient.json(Map.of("email", martaUser.email())),
                rui.cookie(), rui.csrfToken());                                                          // AC07, D09
        api.send("DELETE", "/api/projects/" + website + "/members/" + saraUser.id(), null, ana.cookie(),
                ana.csrfToken());                                                                        // AC08, D10

        assertThat(names(task())).containsExactly("Design " + suffix, "QA " + suffix);
    }

    @Test
    void aMalformedAssigneeIsRefused() {
        List<String> bodies = List.of(                                                                    // DoD07
                "{\"type\":\"robot\",\"userId\":\"" + ruiUser.id() + "\"}",
                "{\"type\":\"person\",\"teamId\":\"" + UUID.randomUUID() + "\"}",
                "{\"type\":\"person\",\"userId\":\"" + ruiUser.id() + "\",\"teamId\":\"" + UUID.randomUUID() + "\"}",
                "{\"userId\":\"" + ruiUser.id() + "\"}",
                "{\"type\":\"person\",\"userId\":\"" + ruiUser.id() + "\",\"name\":\"Rui\"}",
                "{\"type\":\"person\",\"userId\":42}");

        for (String body : bodies) {
            Response response = api.send("POST", path() + "/assignees", body, ana.cookie(), ana.csrfToken());
            assertThat(response.status()).as(body).isEqualTo(400);
            assertThat(response.field("detail")).isEqualTo("The request body is not valid.");
        }
        assertThat(api.send("POST", path() + "/assignees", ApiClient.json(Map.of("type", "person", "userId",
                ruiUser.id().toString())), ana.cookie(), null).status()).isEqualTo(403);                // DoD06
        assertThat(names(task())).isEmpty();
    }

    @Test
    void aTaskThatIsntInTheProjectIsNotFound() {
        Response response = api.send("POST", "/api/projects/" + website + "/tasks/" + UUID.randomUUID() + "/assignees",
                ApiClient.json(Map.of("type", "person", "userId", ruiUser.id().toString())), ana.cookie(), ana.csrfToken());

        assertThat(response.status()).isEqualTo(404);                                                    // DoD05
        assertThat(response.field("detail")).isEqualTo("Task not found.");
    }

    // ------------------------------------------------------------------ who can be assigned

    @Test
    void theEligibleAssigneesAreTheMembersAndTheTeamsAllInTheProject() {
        UUID design = team("Design", rui, saraUser.email());
        UUID marketing = team("Marketing", rui, martaUser.email());

        Response response = api.send("GET", "/api/projects/" + website + "/eligible-assignees", null, rui.cookie(), null);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().get("people").valueStream().map(person -> person.get("name").asString()))
                .containsExactly("Ana Silva", "Rui Costa", "Sara Lopes");
        List<String> teams = response.json().get("teams").valueStream().map(team -> team.get("teamId").asString()).toList();
        assertThat(teams).contains(design.toString()).doesNotContain(marketing.toString());
        assertThat(api.send("GET", "/api/projects/" + website + "/eligible-assignees", null, marta.cookie(), null)
                .status()).isEqualTo(404);                                                               // DoD03
    }

    // ------------------------------------------------------------------ helpers

    /** A team managed by {@code manager}, with another member besides the manager. */
    private UUID team(String name, LoggedIn manager, String memberEmail) {
        UUID team = fixtures.createTeam(manager, name + " " + suffix);
        fixtures.addTeamMember(manager, team, memberEmail);
        return team;
    }

    private Response assign(LoggedIn user, String type, UUID id) {
        return fixtures.assign(user, website, testTheSite, type, id);
    }

    private String path() {
        return "/api/projects/" + website + "/tasks/" + testTheSite;
    }

    private JsonNode task() {
        return api.send("GET", path(), null, ana.cookie(), null).json();
    }

    private List<String> boardAssignees() {
        JsonNode board = api.send("GET", "/api/projects/" + website + "/tasks", null, ana.cookie(), null).json();
        return names(board.get(0));
    }

    private static List<String> names(JsonNode task) {
        return task.get("assignees").valueStream().map(assignee -> assignee.get("name").asString()).toList();
    }
}
