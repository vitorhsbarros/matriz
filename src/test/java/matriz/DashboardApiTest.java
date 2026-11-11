package matriz;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
 * Increment I2: the home dashboard and the search (US04, US23; D14), through real HTTP requests. Each test
 * creates its own users, so the lists hold only that test's projects.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import(TestcontainersConfiguration.class)
class DashboardApiTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PasswordEncoder encoder;

    ApiClient api;
    ProjectFixtures fixtures;
    TestUser marta;
    LoggedIn martaSession;
    TestUser other;
    LoggedIn otherSession;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
        fixtures = new ProjectFixtures(api, jdbc);
        marta = TestUsers.create(jdbc, encoder, "Marta Reis");
        martaSession = api.logInAs(marta.email(), TestUsers.PASSWORD);
        other = TestUsers.create(jdbc, encoder, "Tiago Nunes");
        otherSession = api.logInAs(other.email(), TestUsers.PASSWORD);
    }

    // ------------------------------------------------------------------ US04

    @Test
    void theDashboardShowsOnlyMyProjectsWithMyRoleSortedByName() {
        fixtures.createProject(martaSession, "Website");
        UUID app = fixtures.createProject(otherSession, "App");
        fixtures.addMember(app, marta.id());
        fixtures.createProject(otherSession, "Shop");

        JsonNode projects = dashboard(martaSession, null).json().get("projects");                         // AC01

        assertThat(names(projects)).containsExactly("App", "Website");
        assertThat(projects.get(0).get("role").asString()).isEqualTo("MEMBER");
        assertThat(projects.get(1).get("role").asString()).isEqualTo("MANAGER");
        assertThat(projects.get(0).get("id").asString()).isEqualTo(app.toString());
    }

    @Test
    void notAMemberOfAnyProject() {
        Response response = dashboard(martaSession, null);                                               // AC03

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().get("projects").isEmpty()).isTrue();
        assertThat(counts(response.json().get("totals"))).containsExactly(0, 0, 0);
    }

    @Test
    void aRemovedMemberNoLongerSeesTheProjectOrCountsItsTasks() {
        UUID app = fixtures.createProject(otherSession, "App");
        fixtures.addMember(app, marta.id());
        fixtures.addTasks(martaSession, app, "TODO", 2);
        assertThat(counts(dashboard(martaSession, null).json().get("totals"))).containsExactly(2, 0, 0);

        fixtures.removeMember(app, marta.id());                                                          // AC04

        Response after = dashboard(martaSession, null);
        assertThat(after.json().get("projects").isEmpty()).isTrue();
        assertThat(counts(after.json().get("totals"))).containsExactly(0, 0, 0);
        Response open = api.send("GET", "/api/projects/" + app, null, martaSession.cookie(), null);
        assertThat(open.status()).isEqualTo(404);
        assertThat(open.field("detail")).isEqualTo("Project not found.");
    }

    @Test
    void sortingIgnoresCase() {
        fixtures.createProject(martaSession, "beta");
        fixtures.createProject(martaSession, "Alpha");
        fixtures.createProject(martaSession, "Gamma");

        assertThat(names(dashboard(martaSession, null).json().get("projects")))                            // AC05
                .containsExactly("Alpha", "beta", "Gamma");
    }

    @Test
    void projectsWithTheSameNameAreShownSeparately() {
        UUID first = fixtures.createProject(martaSession, "Website");
        UUID second = fixtures.createProject(martaSession, "Website");
        fixtures.addTasks(martaSession, first, "DONE", 1);

        JsonNode projects = dashboard(martaSession, null).json().get("projects");                         // AC06

        assertThat(names(projects)).containsExactly("Website", "Website");
        List<String> ids = List.of(projects.get(0).get("id").asString(), projects.get(1).get("id").asString());
        assertThat(ids).containsExactlyInAnyOrder(first.toString(), second.toString());
        int firstIndex = ids.indexOf(first.toString());
        assertThat(counts(projects.get(firstIndex).get("taskCounts"))).containsExactly(0, 0, 1);
        assertThat(counts(projects.get(1 - firstIndex).get("taskCounts"))).containsExactly(0, 0, 0);
    }

    @Test
    void taskCountsPerProjectAndInTotal() {
        UUID app = fixtures.createProject(martaSession, "App");
        UUID website = fixtures.createProject(martaSession, "Website");
        fixtures.addTasks(martaSession, app, "TODO", 4);                                                             // AC07
        fixtures.addTasks(martaSession, app, "IN_PROGRESS", 1);
        fixtures.addTasks(martaSession, app, "DONE", 7);
        fixtures.addTasks(martaSession, website, "TODO", 3);
        fixtures.addTasks(martaSession, website, "IN_PROGRESS", 2);
        fixtures.addTasks(martaSession, website, "DONE", 5);

        JsonNode dashboard = dashboard(martaSession, null).json();

        assertThat(counts(dashboard.get("projects").get(0).get("taskCounts"))).containsExactly(4, 1, 7);
        assertThat(counts(dashboard.get("projects").get(1).get("taskCounts"))).containsExactly(3, 2, 5);
        assertThat(counts(dashboard.get("totals"))).containsExactly(7, 3, 12);
    }

    @Test
    void aProjectWithoutTasksShowsZeros() {
        fixtures.createProject(martaSession, "Intranet");                                                // AC08

        JsonNode project = dashboard(martaSession, null).json().get("projects").get(0);

        assertThat(counts(project.get("taskCounts"))).containsExactly(0, 0, 0);
    }

    @Test
    void countsReflectOtherMembersChanges() {
        UUID website = fixtures.createProject(martaSession, "Website");
        UUID task = fixtures.addTask(martaSession, website, "Test the site", "TODO");
        assertThat(counts(dashboard(martaSession, null).json().get("totals"))).containsExactly(1, 0, 0);

        fixtures.setStatus(martaSession, website, task, "DONE");                                                               // AC09

        assertThat(counts(dashboard(martaSession, null).json().get("totals"))).containsExactly(0, 0, 1);
    }

    @Test
    void theDashboardNeedsASession() {
        Response response = api.send("GET", "/api/projects", null, null, null);

        assertThat(response.status()).isEqualTo(401);                                                    // DoD02
        assertThat(response.field("detail")).isEqualTo("Please log in to continue.");
    }

    @Test
    void ruisDashboardShowsHisRoleInEachDemoProject() {
        LoggedIn rui = api.logInAs("rui@example.com", "test-password-r");

        JsonNode projects = dashboard(rui, null).json().get("projects");

        assertThat(projects.valueStream()
                .filter(project -> List.of("App", "Website").contains(project.get("name").asString()))
                .map(project -> project.get("name").asString() + ":" + project.get("role").asString()))
                .contains("App:MANAGER", "Website:MEMBER");
    }

    // ------------------------------------------------------------------ US23

    @Test
    void searchByPartOfTheName() {
        threeProjects();

        assertThat(names(dashboard(martaSession, "web").json().get("projects")))                         // AC01
                .containsExactly("Web shop", "Website");
        assertThat(names(dashboard(martaSession, "  WEB  ").json().get("projects")))                     // D01
                .containsExactly("Web shop", "Website");
    }

    @Test
    void noProjectMatches() {
        threeProjects();

        Response response = dashboard(martaSession, "Intranet");                                         // AC02

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().get("projects").isEmpty()).isTrue();
    }

    @Test
    void otherPeoplesProjectsArentSearched() {
        threeProjects();
        fixtures.createProject(otherSession, "Website 2");

        assertThat(names(dashboard(martaSession, "Website").json().get("projects")))                     // AC03
                .containsExactly("Website");
    }

    @Test
    void anEmptySearchShowsEveryProject() {
        threeProjects();

        assertThat(names(dashboard(martaSession, "").json().get("projects")))                            // AC04
                .containsExactly("App", "Web shop", "Website");
        assertThat(names(dashboard(martaSession, "   ").json().get("projects")))
                .containsExactly("App", "Web shop", "Website");
    }

    @Test
    void totalsDontChangeWithTheSearch() {
        threeProjects();

        JsonNode all = dashboard(martaSession, null).json();
        JsonNode searched = dashboard(martaSession, "App").json();                                       // AC05, D14

        assertThat(names(searched.get("projects"))).containsExactly("App");
        assertThat(counts(searched.get("totals"))).containsExactly(3, 2, 1).isEqualTo(counts(all.get("totals")));
    }

    @Test
    void searchCharactersAreTakenLiterally() {
        threeProjects();

        assertThat(dashboard(martaSession, "%").json().get("projects").isEmpty()).isTrue();
        assertThat(dashboard(martaSession, "_").json().get("projects").isEmpty()).isTrue();
    }

    // ------------------------------------------------------------------ helpers

    /** "App", "Website" and "Web shop", with one task each in To do, In progress and Done order. */
    private void threeProjects() {
        UUID app = fixtures.createProject(martaSession, "App");
        UUID website = fixtures.createProject(martaSession, "Website");
        UUID shop = fixtures.createProject(martaSession, "Web shop");
        fixtures.addTasks(martaSession, app, "TODO", 1);
        fixtures.addTasks(martaSession, website, "TODO", 1);
        fixtures.addTasks(martaSession, website, "IN_PROGRESS", 1);
        fixtures.addTasks(martaSession, shop, "TODO", 1);
        fixtures.addTasks(martaSession, shop, "IN_PROGRESS", 1);
        fixtures.addTasks(martaSession, shop, "DONE", 1);
    }

    private Response dashboard(LoggedIn user, String name) {
        String query = name == null ? "" : "?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        return api.send("GET", "/api/projects" + query, null, user.cookie(), null);
    }

    private static List<String> names(JsonNode projects) {
        return projects.valueStream().map(project -> project.get("name").asString()).toList();
    }

    private static List<Integer> counts(JsonNode counts) {
        return List.of(counts.get("toDo").asInt(), counts.get("inProgress").asInt(), counts.get("done").asInt());
    }
}
