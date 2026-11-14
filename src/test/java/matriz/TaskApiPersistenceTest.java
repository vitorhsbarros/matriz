package matriz;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import matriz.ApiClient.LoggedIn;
import matriz.ApiClient.Response;
import matriz.TestUsers.TestUser;
import org.junit.jupiter.api.AfterEach;
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
 * Creating and reading a task through the running application, against a real PostgreSQL database.
 *
 * <p>Each test creates its own users and project, and removes them when it ends: the number of projects, tasks
 * and users is checked to be the same afterwards as before.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TaskApiPersistenceTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PasswordEncoder encoder;

    ApiClient api;
    ProjectFixtures fixtures;
    TestUser memberUser;
    TestUser outsiderUser;
    LoggedIn member;
    LoggedIn outsider;
    UUID project;
    String tasks;
    Map<String, Integer> rowsBefore;

    @BeforeEach
    void createOwnUsersAndProject() {
        rowsBefore = rowCounts();
        api = new ApiClient(port);
        fixtures = new ProjectFixtures(api, jdbc);
        String unique = UUID.randomUUID().toString().substring(0, 8);
        memberUser = TestUsers.create(jdbc, encoder, "Ana Teixeira " + unique);
        outsiderUser = TestUsers.create(jdbc, encoder, "Marta Reis " + unique);
        member = api.logInAs(memberUser.email(), TestUsers.PASSWORD);
        outsider = api.logInAs(outsiderUser.email(), TestUsers.PASSWORD);
        project = fixtures.createProject(member, "Website " + unique);
        tasks = "/api/projects/" + project + "/tasks";
    }

    @AfterEach
    void removeOwnDataAndCheckTheDatabaseIsBackAsItWas() {
        assertThat(api.send("DELETE", "/api/projects/" + project, null, member.cookie(), member.csrfToken()).status())
                .isEqualTo(204);
        for (LoggedIn user : List.of(member, outsider)) {
            api.send("DELETE", "/api/session", null, user.cookie(), user.csrfToken());
        }
        for (TestUser user : List.of(memberUser, outsiderUser)) {
            jdbc.sql("delete from users where id = ?").param(user.id()).update();
        }

        assertThat(rowCounts()).isEqualTo(rowsBefore);
    }

    @Test
    void aMemberCreatesAValidTaskAndReadsItBack() {
        LocalDate dueDate = LocalDate.now().plusDays(18);

        Response created = api.send("POST", tasks, ApiClient.json(Map.of(
                "title", "Design home page",
                "description", "Follow the style guide",
                "dueDate", dueDate.toString(),
                "priority", "HIGH")), member.cookie(), member.csrfToken());

        assertThat(created.status()).isEqualTo(201);
        JsonNode task = created.json();
        UUID id = UUID.fromString(task.get("id").asString());
        assertIsTheNewTask(task, id, dueDate);

        Response read = api.send("GET", tasks + "/" + id, null, member.cookie(), null);

        assertThat(read.status()).isEqualTo(200);
        assertIsTheNewTask(read.json(), id, dueDate);
        assertThat(read.json().get("createdAt").asString()).isEqualTo(task.get("createdAt").asString());

        Map<String, Object> stored = jdbc.sql(
                        "select project_id, title, description, due_date, status, priority from task where id = ?")
                .param(id)
                .query()
                .singleRow();
        assertThat(stored.get("project_id")).isEqualTo(project);
        assertThat(stored.get("title")).isEqualTo("Design home page");
        assertThat(stored.get("description")).isEqualTo("Follow the style guide");
        assertThat(stored.get("due_date").toString()).isEqualTo(dueDate.toString());
        assertThat(stored.get("status")).isEqualTo("TODO");
        assertThat(stored.get("priority")).isEqualTo("HIGH");
        assertThat(fixtures.count("task_assignee_user", "task_id", id)).isZero();
        assertThat(fixtures.count("task_assignee_team", "task_id", id)).isZero();
    }

    @Test
    void someoneWhoIsNotAMemberCannotReadTheTask() {
        UUID id = fixtures.addTask(member, project, "Design home page", "TODO");

        Response read = api.send("GET", tasks + "/" + id, null, outsider.cookie(), null);

        assertIsRefusedWith(read, 404, "Project not found.");
        assertThat(read.body()).doesNotContain("Design home page");
        for (String taskField : List.of("id", "projectId", "description", "dueDate", "priority", "assignees",
                "createdAt")) {
            assertThat(read.json().has(taskField)).as("the answer has no task field \"%s\"", taskField).isFalse();
        }
    }

    @Test
    void aTitleThatIsTooShortIsRefusedAndNothingIsStored() {
        Response created = api.send("POST", tasks, ApiClient.json(Map.of("title", "ab")),
                member.cookie(), member.csrfToken());

        assertIsRefusedWith(created, 422, "Task title must be at least 3 characters.");
        assertThat(fixtures.count("task", "project_id", project)).isZero();
    }

    @Test
    void aTaskThatWasDeletedIsNotFoundAndNothingChanges() {
        UUID deleted = fixtures.addTask(member, project, "Review copy", "TODO");
        UUID kept = fixtures.addTask(member, project, "Test the site", "TODO");
        assertThat(api.send("DELETE", tasks + "/" + deleted, null, member.cookie(), member.csrfToken()).status())
                .isEqualTo(204);
        List<Map<String, Object>> before = storedTasks();

        Response read = api.send("GET", tasks + "/" + deleted, null, member.cookie(), null);

        assertIsRefusedWith(read, 404, "Task not found.");
        assertThat(storedTasks()).isEqualTo(before);
        assertThat(before).extracting(row -> row.get("id")).containsExactly(kept);
    }

    @Test
    void aTaskIdentifierThatNeverExistedIsNotFoundAndNothingChanges() {
        fixtures.addTask(member, project, "Test the site", "TODO");
        List<Map<String, Object>> before = storedTasks();

        Response read = api.send("GET", tasks + "/" + UUID.randomUUID(), null, member.cookie(), null);

        assertIsRefusedWith(read, 404, "Task not found.");
        assertThat(storedTasks()).isEqualTo(before);
    }

    private void assertIsTheNewTask(JsonNode task, UUID id, LocalDate dueDate) {
        assertThat(task.get("id").asString()).isEqualTo(id.toString());
        assertThat(task.get("projectId").asString()).isEqualTo(project.toString());
        assertThat(task.get("title").asString()).isEqualTo("Design home page");
        assertThat(task.get("description").asString()).isEqualTo("Follow the style guide");
        assertThat(task.get("dueDate").asString()).isEqualTo(dueDate.toString());
        assertThat(task.get("status").asString()).isEqualTo("TODO");
        assertThat(task.get("priority").asString()).isEqualTo("HIGH");
        assertThat(task.get("assignees").isArray()).isTrue();
        assertThat(task.get("assignees").size()).isZero();
        assertThat(OffsetDateTime.parse(task.get("createdAt").asString())).isNotNull();
    }

    private static void assertIsRefusedWith(Response response, int status, String message) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.raw().headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(response.json().get("status").asInt()).isEqualTo(status);
        assertThat(response.field("detail")).isEqualTo(message);
    }

    private List<Map<String, Object>> storedTasks() {
        return new ArrayList<>(jdbc.sql("select * from task where project_id = ? order by id")
                .param(project)
                .query()
                .listOfRows());
    }

    private Map<String, Integer> rowCounts() {
        return Map.of(
                "project", count("project"),
                "task", count("task"),
                "users", count("users"));
    }

    private int count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Integer.class).single();
    }
}
