package matriz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Regression tests for the problems found in the delivery review (docs/implementation-plan.md, "Review for the
 * delivery"):
 *
 * <ul>
 *   <li>R1: a body that isn't JSON got a 415 with a technical message, instead of DoD07's 400.
 *   <li>R2: a creation or assignment racing with a deletion ended in a 500, or made the deletion fail.
 * </ul>
 *
 * <p>Each race is forced: one transaction stops half-way while the HTTP request runs against it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import(TestcontainersConfiguration.class)
class ReviewRegressionTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    TransactionTemplate transactions;

    ApiClient api;
    ProjectFixtures fixtures;
    LoggedIn ana;
    UUID website;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
        fixtures = new ProjectFixtures(api, jdbc);
        TestUser anaUser = TestUsers.create(jdbc, encoder, "Ana Teixeira");
        ana = api.logInAs(anaUser.email(), TestUsers.PASSWORD);
        website = fixtures.createProject(ana, "Website");
    }

    /** R1, DoD07. */
    @Test
    void aBodyThatIsntJsonGetsTheMalformedBodyMessage() {
        Response project = api.send("POST", "/api/projects", "name=Website", "text/plain", ana.cookie(), ana.csrfToken());
        Response task = api.send("POST", "/api/projects/" + website + "/tasks", "<task/>", "application/xml",
                ana.cookie(), ana.csrfToken());

        for (Response response : new Response[] {project, task}) {
            assertThat(response.status()).isEqualTo(400);
            assertThat(response.field("detail")).isEqualTo("The request body is not valid.");
        }
        assertThat(fixtures.count("task", "project_id", website)).isZero();
    }

    /** R2: a task saved while the project is being deleted is deleted with it, and the deletion succeeds. */
    @Test
    void aTaskCreatedWhileItsProjectIsDeletedGoesWithIt() throws Exception {
        UUID racing = UUID.randomUUID();

        Response deletion = whileHeld(
                () -> jdbc.sql("insert into task (id, project_id, title) values (?, ?, 'Racing task')")
                        .params(racing, website).update(),
                () -> api.send("DELETE", "/api/projects/" + website, null, ana.cookie(), ana.csrfToken()));

        assertThat(deletion.status()).isEqualTo(204);
        assertThat(fixtures.count("project", "id", website)).isZero();
        assertThat(fixtures.count("task", "id", racing)).isZero();
    }

    /** R2: a team assigned while it is being deleted leaves the task too, and the deletion succeeds. */
    @Test
    void aTeamAssignedWhileItIsDeletedLeavesTheTask() throws Exception {
        UUID team = fixtures.createTeam(ana, "Design " + UUID.randomUUID().toString().substring(0, 8));
        UUID task = fixtures.addTask(ana, website, "Create logo", "TODO");

        Response deletion = whileHeld(
                () -> jdbc.sql("insert into task_assignee_team (task_id, team_id) values (?, ?)").params(task, team).update(),
                () -> api.send("DELETE", "/api/teams/" + team, null, ana.cookie(), ana.csrfToken()));

        assertThat(deletion.status()).isEqualTo(204);
        assertThat(fixtures.count("team", "id", team)).isZero();
        assertThat(fixtures.count("task_assignee_team", "task_id", task)).isZero();
    }

    /** R2: a task saved just after its project was deleted gets "Project not found.", not a 500. */
    @Test
    void aTaskSavedAfterItsProjectWasDeletedGetsProjectNotFound() throws Exception {
        Response creation = whileHeld(
                () -> {
                    jdbc.sql("delete from project where id = ?").param(website).update();
                },
                () -> api.send("POST", "/api/projects/" + website + "/tasks",
                        ApiClient.json(Map.of("title", "Late task")), ana.cookie(), ana.csrfToken()));

        assertThat(creation.status()).isEqualTo(404);
        assertThat(creation.field("detail")).isEqualTo("Project not found.");
    }

    /** R2: a comment saved just after its task was deleted gets "Task not found.", not a 500. */
    @Test
    void aCommentSavedAfterItsTaskWasDeletedGetsTaskNotFound() throws Exception {
        UUID task = fixtures.addTask(ana, website, "Create logo", "TODO");

        Response comment = whileHeld(
                () -> {
                    jdbc.sql("delete from task where id = ?").param(task).update();
                },
                () -> api.send("POST", "/api/projects/" + website + "/tasks/" + task + "/comments",
                        ApiClient.json(Map.of("text", "Late comment")), ana.cookie(), ana.csrfToken()));

        assertThat(comment.status()).isEqualTo(404);
        assertThat(comment.field("detail")).isEqualTo("Task not found.");
    }

    /**
     * Runs {@code first} in a transaction that stays open while {@code request} is sent, then commits it, and
     * returns the request's answer. The request has to wait for the open transaction wherever their rows meet.
     */
    private Response whileHeld(Runnable first, Supplier<Response> request) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        CountDownLatch requestSent = new CountDownLatch(1);
        CompletableFuture<Void> held = CompletableFuture.runAsync(() -> transactions.executeWithoutResult(status -> {
            first.run();
            done.countDown();
            await(requestSent);
            sleep(600);   // the request is now waiting for this transaction
        }));
        await(done);
        CompletableFuture<Response> answer = CompletableFuture.supplyAsync(() -> {
            requestSent.countDown();
            return request.get();
        });
        held.get(15, TimeUnit.SECONDS);
        return answer.get(15, TimeUnit.SECONDS);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
