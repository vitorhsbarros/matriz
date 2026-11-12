package matriz;

import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * The three automated checks of docs/verification.md, one per case: a validation rule (case 04), an
 * authorization scenario with identifiers changed in the request (case 08), and a creation followed by a look at
 * what was stored (case 03). Real HTTP requests against PostgreSQL; every user and project is made up.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import(TestcontainersConfiguration.class)
class VerificationScenariosTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PasswordEncoder encoder;

    ApiClient api;
    ProjectFixtures fixtures;
    TestUser anaUser;
    LoggedIn ana;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
        fixtures = new ProjectFixtures(api, jdbc);
        anaUser = TestUsers.create(jdbc, encoder, "Ana Teixeira");
        ana = api.logInAs(anaUser.email(), TestUsers.PASSWORD);
    }

    /** Case 04 (US05 AC03, D01): a title made only of spaces is refused, and nothing is stored. */
    @Test
    void case04_aTitleOfOnlySpacesIsRefusedAndNothingIsStored() {
        UUID website = fixtures.createProject(ana, "Website");

        Response response = api.send("POST", "/api/projects/" + website + "/tasks",
                ApiClient.json(Map.of("title", "     ", "priority", "HIGH")), ana.cookie(), ana.csrfToken());

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.field("detail")).isEqualTo("Task title is required.");
        assertThat(fixtures.count("task", "project_id", website)).isZero();
    }

    /**
     * Case 08 (DoD03, DoD05, SEC11): changing the identifiers in the request gets no access to anything. A user
     * who isn't a member uses the project's real identifiers; a member of another project puts this project's task
     * under their own project's address; a member who isn't the author targets someone else's comment. Nothing
     * changes in the database.
     */
    @Test
    void case08_changingIdentifiersInTheRequestGetsNoAccess() {
        UUID website = fixtures.createProject(ana, "Website");
        UUID task = fixtures.addTask(ana, website, "Create logo", "TODO");
        UUID comment = UUID.fromString(api.send("POST", "/api/projects/" + website + "/tasks/" + task + "/comments",
                ApiClient.json(Map.of("text", "First draft is ready")), ana.cookie(), ana.csrfToken())
                .json().get("id").asString());

        TestUser saraUser = TestUsers.create(jdbc, encoder, "Sara Pinto");      // not a member of Website
        LoggedIn sara = api.logInAs(saraUser.email(), TestUsers.PASSWORD);
        TestUser ruiUser = TestUsers.create(jdbc, encoder, "Rui Moreira");      // member of Website, manages App
        LoggedIn rui = api.logInAs(ruiUser.email(), TestUsers.PASSWORD);
        fixtures.addMember(ana, website, ruiUser.email());
        UUID app = fixtures.createProject(rui, "App");

        Response saraReads = api.send("GET", "/api/projects/" + website + "/tasks/" + task, null, sara.cookie(), null);
        Response saraEdits = api.send("PATCH", "/api/projects/" + website + "/tasks/" + task,
                ApiClient.json(Map.of("title", "Sara's title")), sara.cookie(), sara.csrfToken());
        Response ruiUnderApp = api.send("PATCH", "/api/projects/" + app + "/tasks/" + task,
                ApiClient.json(Map.of("status", "DONE")), rui.cookie(), rui.csrfToken());
        Response ruiDeletesAnasComment = api.send("DELETE",
                "/api/projects/" + website + "/tasks/" + task + "/comments/" + comment, null, rui.cookie(), rui.csrfToken());

        assertThat(saraReads.status()).isEqualTo(404);
        assertThat(saraReads.field("detail")).isEqualTo("Project not found.");
        assertThat(saraReads.body()).doesNotContain("Create logo");
        assertThat(saraEdits.status()).isEqualTo(404);
        assertThat(ruiUnderApp.status()).isEqualTo(404);
        assertThat(ruiUnderApp.field("detail")).isEqualTo("Task not found.");
        assertThat(ruiDeletesAnasComment.status()).isEqualTo(403);
        assertThat(ruiDeletesAnasComment.field("detail")).isEqualTo("Only the author can change this comment.");

        Map<String, Object> stored = jdbc.sql("select title, status, project_id from task where id = ?")
                .param(task).query().singleRow();
        assertThat(stored).containsEntry("title", "Create logo").containsEntry("status", "TODO")
                .containsEntry("project_id", website);
        assertThat(fixtures.count("task_comment", "id", comment)).isEqualTo(1);
    }

    /** Case 03 (US05 AC01, US06): a created task is stored with what was sent, and the board lists it. */
    @Test
    void case03_aCreatedTaskIsStoredAndListed() {
        UUID website = fixtures.createProject(ana, "Website");
        String dueDate = java.time.LocalDate.now().plusDays(7).toString();

        Response created = api.send("POST", "/api/projects/" + website + "/tasks",
                ApiClient.json(Map.of("title", "  Design home page  ", "description", "Follow the style guide",
                        "dueDate", dueDate, "priority", "HIGH")), ana.cookie(), ana.csrfToken());

        assertThat(created.status()).isEqualTo(201);
        UUID id = UUID.fromString(created.json().get("id").asString());
        Map<String, Object> row = jdbc.sql("""
                select project_id, title, description, due_date::text as due_date, status, priority, created_at
                from task where id = ?
                """).param(id).query().singleRow();
        assertThat(row)
                .containsEntry("project_id", website)
                .containsEntry("title", "Design home page")               // trimmed (D01)
                .containsEntry("description", "Follow the style guide")
                .containsEntry("due_date", dueDate)
                .containsEntry("status", "TODO")                          // a new task starts in To do
                .containsEntry("priority", "HIGH");
        assertThat(row.get("created_at")).isNotNull();
        assertThat(fixtures.count("task_assignee_user", "task_id", id)).isZero();   // "Unassigned"

        Response board = api.send("GET", "/api/projects/" + website + "/tasks", null, ana.cookie(), null);
        assertThat(board.json().get(0).get("id").asString()).isEqualTo(id.toString());
    }
}
