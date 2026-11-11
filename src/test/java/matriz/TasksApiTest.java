package matriz;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
 * Increment I3: a project's tasks (US05, US06, US07, US08, US20, US16, US24), through real HTTP requests.
 * Every test has its own project, with a manager (Ana's role), another member (Rui's role) and someone who
 * isn't a member (Marta's role in the stories). Dates are relative to the server's today (AS11).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import(TestcontainersConfiguration.class)
class TasksApiTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PasswordEncoder encoder;

    ApiClient api;
    ProjectFixtures fixtures;
    LoggedIn ana;
    LoggedIn rui;
    LoggedIn marta;
    UUID website;
    String tasks;
    LocalDate today;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
        fixtures = new ProjectFixtures(api, jdbc);
        TestUser manager = TestUsers.create(jdbc, encoder, "Ana Teixeira");
        TestUser member = TestUsers.create(jdbc, encoder, "Rui Moreira");
        TestUser outsider = TestUsers.create(jdbc, encoder, "Marta Reis");
        ana = api.logInAs(manager.email(), TestUsers.PASSWORD);
        rui = api.logInAs(member.email(), TestUsers.PASSWORD);
        marta = api.logInAs(outsider.email(), TestUsers.PASSWORD);
        website = fixtures.createProject(ana, "Website");
        fixtures.addMember(website, member.id());
        tasks = "/api/projects/" + website + "/tasks";
        today = LocalDate.now();
    }

    // ------------------------------------------------------------------ US05 — create

    @Test
    void createATaskWithEveryField() {
        Response created = create(ana, Map.of("title", "Design home page", "description", "Follow the style guide",
                "dueDate", today.plusDays(18).toString(), "priority", "HIGH"));                          // AC01

        assertThat(created.status()).isEqualTo(201);
        JsonNode task = created.json();
        assertThat(task.get("title").asString()).isEqualTo("Design home page");
        assertThat(task.get("description").asString()).isEqualTo("Follow the style guide");
        assertThat(task.get("dueDate").asString()).isEqualTo(today.plusDays(18).toString());
        assertThat(task.get("priority").asString()).isEqualTo("HIGH");
        assertThat(task.get("status").asString()).isEqualTo("TODO");
        assertThat(task.get("assignees").isEmpty()).isTrue();                                           // "Unassigned"
        assertThat(task.get("projectId").asString()).isEqualTo(website.toString());
        assertThat(task.get("createdAt").asString()).isNotBlank();
        assertThat(created.location()).isEqualTo(tasks + "/" + task.get("id").asString());
        assertThat(titles(list(ana, ""))).containsExactly("Design home page");
    }

    @Test
    void createATaskWithOnlyATitle() {
        JsonNode task = create(ana, Map.of("title", "Review copy")).json();                              // AC02

        assertThat(task.get("description").isNull()).isTrue();
        assertThat(task.get("dueDate").isNull()).isTrue();
        assertThat(task.get("priority").asString()).isEqualTo("MEDIUM");
    }

    @Test
    void theTitleIsRequired() {
        for (String body : List.of("{\"title\":\"\"}", "{\"title\":\"   \"}", "{}", "{\"title\":null}")) {  // AC03, D01
            Response response = api.send("POST", tasks, body, ana.cookie(), ana.csrfToken());

            assertThat(response.status()).as(body).isEqualTo(422);
            assertThat(response.field("detail")).as(body).isEqualTo("Task title is required.");
        }
        assertThat(list(ana, "").json().isEmpty()).isTrue();
    }

    @Test
    void aDueDateInThePastIsRefused() {
        Response response = create(ana, Map.of("title", "Review copy", "dueDate", today.minusDays(1).toString()));

        assertThat(response.status()).isEqualTo(422);                                                    // AC04
        assertThat(response.field("detail")).isEqualTo("Due date cannot be in the past.");
        assertThat(list(ana, "").json().isEmpty()).isTrue();
    }

    @Test
    void aDateThatDoesntExistIsRefused() {
        for (String date : List.of("2027-02-30", "20-11-2026", "tomorrow")) {                            // AC05
            Response response = create(ana, Map.of("title", "Review copy", "dueDate", date));

            assertThat(response.status()).as(date).isEqualTo(422);
            assertThat(response.field("detail")).as(date).isEqualTo("Enter a valid date.");
        }
        assertThat(list(ana, "").json().isEmpty()).isTrue();
    }

    @Test
    void aNonMemberCantCreateATask() {
        Response response = create(marta, Map.of("title", "Review copy"));                               // AC06, DoD03

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.field("detail")).isEqualTo("Project not found.");
        assertThat(list(ana, "").json().isEmpty()).isTrue();
    }

    @Test
    void aDueDateEqualToTodayIsAccepted() {
        Response created = create(ana, Map.of("title", "Review copy", "dueDate", today.toString()));    // AC07

        assertThat(created.status()).isEqualTo(201);
        assertThat(created.json().get("dueDate").asString()).isEqualTo(today.toString());
    }

    @Test
    void titleAndDescriptionLimits() {
        assertThat(detail(create(ana, Map.of("title", "ab")))).isEqualTo("Task title must be at least 3 characters."); // AC08
        assertThat(detail(create(ana, Map.of("title", "  ab  ")))).isEqualTo("Task title must be at least 3 characters.");
        assertThat(create(ana, Map.of("title", "abc")).status()).isEqualTo(201);
        assertThat(create(ana, Map.of("title", "t".repeat(150))).status()).isEqualTo(201);
        assertThat(detail(create(ana, Map.of("title", "t".repeat(151))))).isEqualTo("Task title must be at most 150 characters.");
        assertThat(create(ana, Map.of("title", "Review copy", "description", "d".repeat(2000))).status()).isEqualTo(201);
        assertThat(detail(create(ana, Map.of("title", "Review copy", "description", "d".repeat(2001)))))
                .isEqualTo("Description must be at most 2000 characters.");
    }

    @Test
    void repeatedTitlesAreAllowed() {
        String first = create(ana, Map.of("title", "Review copy")).json().get("id").asString();         // AC09
        String second = create(ana, Map.of("title", "Review copy")).json().get("id").asString();

        assertThat(second).isNotEqualTo(first);
        assertThat(titles(list(ana, ""))).containsExactly("Review copy", "Review copy");
    }

    @Test
    void anInvalidPriorityIsRefused() {
        for (String priority : List.of("Urgent", "high")) {                                              // AC10
            Response response = create(ana, Map.of("title", "Review copy", "priority", priority));

            assertThat(response.status()).as(priority).isEqualTo(422);
            assertThat(response.field("detail")).as(priority).isEqualTo("Invalid priority.");
        }
    }

    @Test
    void fieldsAreCheckedInTheOrderOfTheSchema() {
        Response response = create(ana, Map.of("title", "", "dueDate", "2027-02-30", "priority", "Urgent"));   // AS08

        assertThat(response.field("detail")).isEqualTo("Task title is required.");
        assertThat(detail(create(ana, Map.of("title", "Review copy", "dueDate", "not a date", "priority", "Urgent"))))
                .isEqualTo("Enter a valid date.");
    }

    @Test
    void createIsRefusedWithoutASessionWithoutTheTokenOrWithAMalformedBody() {
        assertThat(api.send("POST", tasks, "{\"title\":\"Review copy\"}", null, null).status()).isEqualTo(401);     // DoD02
        Response noToken = api.send("POST", tasks, "{\"title\":\"Review copy\"}", ana.cookie(), null);
        assertThat(noToken.status()).isEqualTo(403);                                                                // DoD06
        assertThat(noToken.field("detail")).isEqualTo("The request could not be verified. Please reload and try again.");
        for (String body : List.of("{\"title\":", "{\"title\":42}", "{\"title\":\"Review copy\",\"status\":\"DONE\"}")) {
            Response malformed = api.send("POST", tasks, body, ana.cookie(), ana.csrfToken());
            assertThat(malformed.status()).as(body).isEqualTo(400);                                                 // DoD07
            assertThat(malformed.field("detail")).as(body).isEqualTo("The request body is not valid.");
        }
        assertThat(list(ana, "").json().isEmpty()).isTrue();
    }

    // ------------------------------------------------------------------ US06 — the board's list

    @Test
    void theListShowsTheProjectsTasksNewestFirst() {
        fixtures.addTask(ana, website, "Review copy", "DONE");                                          // AC01
        fixtures.addTask(ana, website, "Create logo", "IN_PROGRESS");
        fixtures.addTask(ana, website, "Test the site", "TODO");

        JsonNode list = list(rui, "").json();

        assertThat(titles(list)).containsExactly("Test the site", "Create logo", "Review copy");        // AC06
        assertThat(list.get(0).get("status").asString()).isEqualTo("TODO");
        assertThat(list.get(1).get("status").asString()).isEqualTo("IN_PROGRESS");
        assertThat(list.get(2).get("status").asString()).isEqualTo("DONE");
        assertThat(list.get(0).get("assignees").isEmpty()).isTrue();
    }

    @Test
    void aProjectWithoutTasksHasAnEmptyList() {
        Response response = list(ana, "");                                                               // AC02

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().isEmpty()).isTrue();
    }

    @Test
    void aNonMemberCantSeeTheTasks() {
        fixtures.addTask(ana, website, "Test the site", "TODO");

        Response response = list(marta, "");                                                             // AC03

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.field("detail")).isEqualTo("Project not found.");
        assertThat(response.body()).doesNotContain("Test the site");
    }

    @Test
    void onlyTheOpenProjectsTasksAreShown() {
        UUID app = fixtures.createProject(ana, "App");
        fixtures.addTask(ana, app, "Publish to the store", "TODO");                                      // AC04

        assertThat(titles(list(ana, ""))).doesNotContain("Publish to the store");
    }

    @Test
    void theListReflectsOtherMembersChanges() {
        UUID task = fixtures.addTask(ana, website, "Test the site", "TODO");

        fixtures.setStatus(rui, website, task, "IN_PROGRESS");                                           // AC05, NFR07

        assertThat(list(ana, "").json().get(0).get("status").asString()).isEqualTo("IN_PROGRESS");
    }

    // ------------------------------------------------------------------ US07 — status

    @Test
    void changingTheStatusChangesNothingElse() {
        JsonNode before = create(ana, Map.of("title", "Test the site", "description", "Test on mobile",
                "dueDate", today.plusDays(5).toString(), "priority", "HIGH")).json();

        JsonNode after = patch(ana, before, Map.of("status", "IN_PROGRESS")).json();                      // AC01

        assertThat(after.get("status").asString()).isEqualTo("IN_PROGRESS");
        for (String field : List.of("title", "description", "dueDate", "priority", "assignees", "createdAt")) {
            assertThat(after.get(field)).as(field).isEqualTo(before.get(field));
        }
    }

    @Test
    void everyTransitionIsAllowed() {
        JsonNode task = create(ana, Map.of("title", "Test the site")).json();
        List<String> path = List.of("IN_PROGRESS", "TODO", "DONE", "TODO", "IN_PROGRESS", "DONE", "IN_PROGRESS");

        for (String status : path) {                                                                     // AC02
            Response moved = patch(ana, task, Map.of("status", status));
            assertThat(moved.status()).as(status).isEqualTo(200);
            assertThat(moved.json().get("status").asString()).isEqualTo(status);
        }
    }

    @Test
    void anInvalidStatusIsRefused() {
        JsonNode task = create(ana, Map.of("title", "Test the site")).json();
        Map<String, Object> nullStatus = new HashMap<>();
        nullStatus.put("status", null);

        for (Map<String, ?> body : List.of(Map.of("status", "Blocked"), Map.of("status", "TO_DO"), nullStatus)) {   // AC04
            Response response = patch(ana, task, body);
            assertThat(response.status()).as(body.toString()).isEqualTo(422);
            assertThat(response.field("detail")).isEqualTo("Invalid status.");
        }
        assertThat(get(ana, task).json().get("status").asString()).isEqualTo("TODO");
    }

    @Test
    void aNonMemberCantChangeTheStatus() {
        JsonNode task = create(ana, Map.of("title", "Test the site")).json();

        Response response = patch(marta, task, Map.of("status", "DONE"));                                 // AC05

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.field("detail")).isEqualTo("Project not found.");
        assertThat(get(ana, task).json().get("status").asString()).isEqualTo("TODO");
    }

    @Test
    void choosingTheCurrentStatusIsFine() {
        JsonNode task = create(ana, Map.of("title", "Test the site")).json();

        Response response = patch(ana, task, Map.of("status", "TODO"));                                   // AC06

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().get("status").asString()).isEqualTo("TODO");
    }

    @Test
    void anyMemberCanChangeAnyTask() {
        JsonNode task = create(ana, Map.of("title", "Test the site")).json();

        Response response = patch(rui, task, Map.of("status", "DONE"));                                   // AC07, D08

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().get("status").asString()).isEqualTo("DONE");
    }

    @Test
    void theLastSavedChangeWins() {
        JsonNode task = create(ana, Map.of("title", "Test the site")).json();

        patch(ana, task, Map.of("status", "IN_PROGRESS"));                                               // US07 AC08, D11
        patch(rui, task, Map.of("status", "DONE"));
        patch(ana, task, Map.of("title", "Title A"));                                                    // US08 AC09
        patch(rui, task, Map.of("title", "Title B"));

        JsonNode finalTask = get(ana, task).json();
        assertThat(finalTask.get("status").asString()).isEqualTo("DONE");
        assertThat(finalTask.get("title").asString()).isEqualTo("Title B");
    }

    // ------------------------------------------------------------------ US08 — edit

    @Test
    void editEveryField() {
        JsonNode task = testTheSite();

        JsonNode edited = patch(ana, task, Map.of("title", "Test the site in production",               // AC01
                "description", "Test on mobile and tablet", "dueDate", today.plusDays(23).toString(),
                "priority", "HIGH")).json();

        assertThat(edited.get("title").asString()).isEqualTo("Test the site in production");
        assertThat(edited.get("description").asString()).isEqualTo("Test on mobile and tablet");
        assertThat(edited.get("dueDate").asString()).isEqualTo(today.plusDays(23).toString());
        assertThat(edited.get("priority").asString()).isEqualTo("HIGH");
        assertThat(edited.get("status").asString()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void clearTheDescriptionAndDueDate() {
        JsonNode task = testTheSite();
        Map<String, Object> clear = new HashMap<>();
        clear.put("description", null);
        clear.put("dueDate", null);

        JsonNode cleared = patch(ana, task, clear).json();                                               // AC02

        assertThat(cleared.get("description").isNull()).isTrue();
        assertThat(cleared.get("dueDate").isNull()).isTrue();
        assertThat(cleared.get("title").asString()).isEqualTo("Test the site");
    }

    @Test
    void aClearedTitleIsRefusedAndNothingChanges() {
        JsonNode task = testTheSite();

        Response cleared = patch(ana, task, Map.of("title", ""));                                         // AC03
        Response mixed = patch(ana, task, Map.of("description", "New description", "title", ""));        // AC05
        Response tooShort = patch(ana, task, Map.of("title", "ab"));

        assertThat(cleared.field("detail")).isEqualTo("Task title is required.");
        assertThat(mixed.status()).isEqualTo(422);
        assertThat(mixed.field("detail")).isEqualTo("Task title is required.");
        assertThat(tooShort.field("detail")).isEqualTo("Task title must be at least 3 characters.");
        assertUnchanged(task);
    }

    @Test
    void aNewDueDateInThePastIsRefused() {
        JsonNode task = testTheSite();

        Response response = patch(ana, task, Map.of("dueDate", today.minusDays(1).toString()));          // AC04

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.field("detail")).isEqualTo("Due date cannot be in the past.");
        assertUnchanged(task);
    }

    @Test
    void aNonMemberCantEdit() {
        JsonNode task = testTheSite();

        Response response = patch(marta, task, Map.of("title", "Marta's title"));                        // AC06

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.field("detail")).isEqualTo("Project not found.");
        assertUnchanged(task);
    }

    @Test
    void aPastDueDateThatIsntChangedStaysValid() {
        JsonNode task = testTheSite();
        LocalDate passed = today.minusDays(3);
        jdbc.sql("update task set due_date = ? where id = ?")                                            // AC07, D12
                .params(passed, UUID.fromString(task.get("id").asString())).update();

        Response titleOnly = patch(ana, task, Map.of("title", "Test the final site"));
        // The interface sends every field of the form, including the unchanged past date.
        Response wholeForm = patch(ana, task, Map.of("title", "Test the final site", "dueDate", passed.toString()));

        assertThat(titleOnly.status()).isEqualTo(200);
        assertThat(wholeForm.status()).isEqualTo(200);
        assertThat(wholeForm.json().get("dueDate").asString()).isEqualTo(passed.toString());
    }

    @Test
    void saveWithoutChanges() {
        JsonNode task = testTheSite();

        Response same = patch(ana, task, Map.of("title", "Test the site", "description", "Test on mobile",   // AC08
                "dueDate", today.plusDays(18).toString(), "priority", "MEDIUM"));
        Response empty = api.send("PATCH", tasks + "/" + task.get("id").asString(), "{}", ana.cookie(), ana.csrfToken());

        assertThat(same.status()).isEqualTo(200);
        assertThat(empty.status()).isEqualTo(200);                                                       // AS20
        assertUnchanged(task);
    }

    @Test
    void anInvalidPriorityIsRefusedWhenEditing() {
        JsonNode task = testTheSite();
        Map<String, Object> nullPriority = new HashMap<>();
        nullPriority.put("priority", null);

        for (Map<String, ?> body : List.of(Map.of("priority", "Urgent"), nullPriority)) {               // AC10
            Response response = patch(ana, task, body);
            assertThat(response.status()).isEqualTo(422);
            assertThat(response.field("detail")).isEqualTo("Invalid priority.");
        }
        assertUnchanged(task);
    }

    @Test
    void editIsRefusedWithoutTheTokenOrWithAMalformedBody() {
        JsonNode task = testTheSite();
        String path = tasks + "/" + task.get("id").asString();

        assertThat(api.send("PATCH", path, "{\"title\":\"New title\"}", ana.cookie(), null).status()).isEqualTo(403);
        assertThat(api.send("PATCH", path, "{\"assignees\":[]}", ana.cookie(), ana.csrfToken()).status()).isEqualTo(400);
        assertThat(api.send("PATCH", path, "{\"priority\":3}", ana.cookie(), ana.csrfToken()).status()).isEqualTo(400);
        assertThat(api.send("PATCH", path, "{\"title\":\"New title\"}", null, null).status()).isEqualTo(401);
        assertUnchanged(task);
    }

    // ------------------------------------------------------------------ US20 — details

    @Test
    void openATask() {
        JsonNode task = create(ana, Map.of("title", "Create logo", "description", "Use the new colours",
                "dueDate", today.plusDays(18).toString(), "priority", "HIGH")).json();
        patch(ana, task, Map.of("status", "IN_PROGRESS"));

        JsonNode details = get(rui, task).json();                                                        // AC01

        assertThat(details.get("title").asString()).isEqualTo("Create logo");
        assertThat(details.get("description").asString()).isEqualTo("Use the new colours");
        assertThat(details.get("status").asString()).isEqualTo("IN_PROGRESS");
        assertThat(details.get("priority").asString()).isEqualTo("HIGH");
        assertThat(details.get("dueDate").asString()).isEqualTo(today.plusDays(18).toString());
    }

    @Test
    void aNonMemberCantOpenATask() {
        JsonNode task = create(ana, Map.of("title", "Create logo")).json();

        Response response = get(marta, task);                                                            // AC02

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.field("detail")).isEqualTo("Project not found.");
        assertThat(response.body()).doesNotContain("Create logo");
    }

    @Test
    void aTaskThatIsntInTheProjectIsNotFound() {
        JsonNode deleted = create(ana, Map.of("title", "Create logo")).json();
        delete(rui, deleted);
        UUID app = fixtures.createProject(ana, "App");
        UUID otherProjectsTask = fixtures.addTask(ana, app, "Publish to the store", "TODO");

        List<Response> responses = List.of(                                                              // AC03, DoD05
                get(ana, deleted),
                api.send("GET", tasks + "/" + otherProjectsTask, null, ana.cookie(), null),
                api.send("PATCH", tasks + "/" + otherProjectsTask, "{\"status\":\"DONE\"}", ana.cookie(), ana.csrfToken()),
                api.send("DELETE", tasks + "/" + otherProjectsTask, null, ana.cookie(), ana.csrfToken()),
                api.send("GET", tasks + "/not-an-identifier", null, ana.cookie(), null),
                api.send("GET", tasks + "/" + UUID.randomUUID(), null, ana.cookie(), null));

        assertThat(responses).allSatisfy(response -> {
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.field("detail")).isEqualTo("Task not found.");
        });
        assertThat(api.send("GET", "/api/projects/" + app + "/tasks/" + otherProjectsTask, null, ana.cookie(), null)
                .json().get("status").asString()).isEqualTo("TODO");
        assertThat(api.send("GET", "/api/projects/not-an-identifier/tasks", null, ana.cookie(), null)
                .field("detail")).isEqualTo("Project not found.");
    }

    @Test
    void openATaskWithOnlyATitle() {
        JsonNode details = get(ana, create(ana, Map.of("title", "Review copy")).json()).json();          // AC04

        assertThat(details.get("description").isNull()).isTrue();
        assertThat(details.get("dueDate").isNull()).isTrue();
        assertThat(details.get("assignees").isEmpty()).isTrue();
    }

    // ------------------------------------------------------------------ US16 — delete

    @Test
    void deleteATaskWithItsAssigneesAndComments() {
        JsonNode testTheSite = create(ana, Map.of("title", "Test the site")).json();
        create(ana, Map.of("title", "Create logo"));
        UUID id = UUID.fromString(testTheSite.get("id").asString());
        fixtures.assignUser(id, UUID.fromString(api.send("GET", "/api/session", null, rui.cookie(), null)
                .json().get("user").get("id").asString()));
        fixtures.addComment(id, UUID.fromString(api.send("GET", "/api/session", null, ana.cookie(), null)
                .json().get("user").get("id").asString()), "Check the forms");

        Response deleted = delete(ana, testTheSite);                                                     // AC02

        assertThat(deleted.status()).isEqualTo(204);
        assertThat(titles(list(rui, ""))).containsExactly("Create logo");
        assertThat(fixtures.count("task_assignee_user", "task_id", id)).isZero();                       // R22
        assertThat(fixtures.count("task_comment", "task_id", id)).isZero();
    }

    @Test
    void aNonMemberCantDeleteATask() {
        JsonNode task = create(ana, Map.of("title", "Test the site")).json();

        Response response = delete(marta, task);                                                         // AC04

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.field("detail")).isEqualTo("Project not found.");
        assertThat(get(ana, task).status()).isEqualTo(200);
    }

    @Test
    void aTaskAlreadyDeletedIsNotFound() {
        JsonNode task = create(ana, Map.of("title", "Test the site")).json();
        delete(rui, task);

        Response again = delete(ana, task);                                                              // AC05

        assertThat(again.status()).isEqualTo(404);
        assertThat(again.field("detail")).isEqualTo("Task not found.");
    }

    @Test
    void anyMemberCanDeleteAnyTaskIncludingTheLast() {
        JsonNode task = create(ana, Map.of("title", "Create logo")).json();

        assertThat(delete(rui, task).status()).isEqualTo(204);                                           // AC06, D08
        assertThat(list(ana, "").json().isEmpty()).isTrue();                                            // AC07
    }

    @Test
    void deleteIsRefusedWithoutTheToken() {
        JsonNode task = create(ana, Map.of("title", "Test the site")).json();

        Response response = api.send("DELETE", tasks + "/" + task.get("id").asString(), null, ana.cookie(), null);

        assertThat(response.status()).isEqualTo(403);                                                    // DoD06
        assertThat(get(ana, task).status()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ US24 — filters

    @Test
    void filterByPriority() {
        filterExample();

        assertThat(titles(list(ana, "?priority=HIGH")))                                                   // AC01
                .containsExactlyInAnyOrder("Test the site", "Create logo");
    }

    @Test
    void filterByStatusAndPriority() {
        filterExample();

        assertThat(titles(list(ana, "?status=TODO&priority=HIGH&priority=LOW")))                         // AC02, AS17
                .containsExactlyInAnyOrder("Test the site", "Write copy");
        assertThat(titles(list(ana, "?status=TODO&status=DONE")))
                .containsExactlyInAnyOrder("Test the site", "Write copy", "Review copy");
    }

    @Test
    void withoutFiltersEveryTaskIsListed() {
        filterExample();

        assertThat(list(ana, "").json().size()).isEqualTo(4);                                            // AC03, AC06
    }

    @Test
    void noTaskMatches() {
        filterExample();

        Response response = list(ana, "?status=DONE&priority=HIGH");                                     // AC04

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.json().isEmpty()).isTrue();
    }

    @Test
    void anInvalidFilterValueIsRefused() {
        Response priority = list(ana, "?priority=Urgent");                                               // AC05
        Response status = list(ana, "?status=Blocked");

        assertThat(priority.status()).isEqualTo(422);
        assertThat(priority.field("detail")).isEqualTo("Invalid priority.");
        assertThat(status.status()).isEqualTo(422);
        assertThat(status.field("detail")).isEqualTo("Invalid status.");
    }

    @Test
    void listingIsRefusedWithoutASession() {
        Response response = api.send("GET", tasks, null, null, null);

        assertThat(response.status()).isEqualTo(401);                                                    // DoD02
        assertThat(response.field("detail")).isEqualTo("Please log in to continue.");
    }

    // ------------------------------------------------------------------ the home dashboard follows the tasks

    @Test
    void theDashboardCountsFollowCreatingMovingAndDeleting() {
        JsonNode first = create(ana, Map.of("title", "Test the site")).json();
        JsonNode second = create(ana, Map.of("title", "Create logo")).json();
        assertThat(websiteCounts(rui)).containsExactly(2, 0, 0);

        patch(rui, first, Map.of("status", "DONE"));
        assertThat(websiteCounts(ana)).containsExactly(1, 0, 1);

        delete(ana, second);
        assertThat(websiteCounts(rui)).containsExactly(0, 0, 1);
    }

    // ------------------------------------------------------------------ helpers

    /** US08's shared context: "Test the site", in progress, due in 18 days. */
    private JsonNode testTheSite() {
        JsonNode task = create(ana, Map.of("title", "Test the site", "description", "Test on mobile",
                "dueDate", today.plusDays(18).toString())).json();
        return patch(ana, task, Map.of("status", "IN_PROGRESS")).json();
    }

    /** US24's shared context. */
    private void filterExample() {
        create(ana, Map.of("title", "Test the site", "priority", "HIGH"));
        create(ana, Map.of("title", "Write copy", "priority", "LOW"));
        patch(ana, create(ana, Map.of("title", "Create logo", "priority", "HIGH")).json(), Map.of("status", "IN_PROGRESS"));
        patch(ana, create(ana, Map.of("title", "Review copy")).json(), Map.of("status", "DONE"));
    }

    private void assertUnchanged(JsonNode before) {
        JsonNode now = get(ana, before).json();
        for (String field : List.of("title", "description", "dueDate", "status", "priority")) {
            assertThat(now.get(field)).as(field).isEqualTo(before.get(field));
        }
    }

    private List<Integer> websiteCounts(LoggedIn user) {
        JsonNode projects = api.send("GET", "/api/projects", null, user.cookie(), null).json().get("projects");
        JsonNode counts = projects.valueStream()
                .filter(project -> project.get("id").asString().equals(website.toString()))
                .findFirst().orElseThrow().get("taskCounts");
        return List.of(counts.get("toDo").asInt(), counts.get("inProgress").asInt(), counts.get("done").asInt());
    }

    private Response create(LoggedIn user, Map<String, ?> body) {
        return api.send("POST", tasks, ApiClient.json(body), user.cookie(), user.csrfToken());
    }

    private Response patch(LoggedIn user, JsonNode task, Map<String, ?> body) {
        return api.send("PATCH", tasks + "/" + task.get("id").asString(), ApiClient.json(body), user.cookie(),
                user.csrfToken());
    }

    private Response get(LoggedIn user, JsonNode task) {
        return api.send("GET", tasks + "/" + task.get("id").asString(), null, user.cookie(), null);
    }

    private Response delete(LoggedIn user, JsonNode task) {
        return api.send("DELETE", tasks + "/" + task.get("id").asString(), null, user.cookie(), user.csrfToken());
    }

    private Response list(LoggedIn user, String query) {
        return api.send("GET", tasks + query, null, user.cookie(), null);
    }

    private static String detail(Response response) {
        return response.field("detail");
    }

    private static List<String> titles(Response response) {
        return titles(response.json());
    }

    private static List<String> titles(JsonNode list) {
        return list.valueStream().map(task -> task.get("title").asString()).toList();
    }
}
