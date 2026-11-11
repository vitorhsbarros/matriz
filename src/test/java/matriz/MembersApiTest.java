package matriz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import matriz.ApiClient.LoggedIn;
import matriz.ApiClient.Response;
import matriz.TestUsers.TestUser;
import matriz.projects.ProjectMembers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * Increment I4: a project's members (US09; D07, D10, SEC09, PRI03; PD05), through real HTTP requests. Each test
 * has its own project, managed by Ana's role, with people who have accounts but aren't members yet.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import(TestcontainersConfiguration.class)
class MembersApiTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    ProjectMembers projectMembers;

    @Autowired
    TransactionTemplate transactions;

    ApiClient api;
    ProjectFixtures fixtures;
    TestUser ruiUser;
    TestUser saraUser;
    LoggedIn ana;
    LoggedIn rui;
    LoggedIn sara;
    UUID website;
    String members;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
        fixtures = new ProjectFixtures(api, jdbc);
        TestUser anaUser = TestUsers.create(jdbc, encoder, "Ana Teixeira");
        ruiUser = TestUsers.create(jdbc, encoder, "Rui Moreira");
        saraUser = TestUsers.create(jdbc, encoder, "Sara Pinto");
        ana = api.logInAs(anaUser.email(), TestUsers.PASSWORD);
        rui = api.logInAs(ruiUser.email(), TestUsers.PASSWORD);
        sara = api.logInAs(saraUser.email(), TestUsers.PASSWORD);
        website = fixtures.createProject(ana, "Website");
        members = "/api/projects/" + website + "/members";
    }

    @Test
    void addAMember() {
        Response added = add(ana, ruiUser.email());                                                      // AC01

        assertThat(added.status()).isEqualTo(201);
        JsonNode member = added.json();
        assertThat(member.get("userId").asString()).isEqualTo(ruiUser.id().toString());
        assertThat(member.get("name").asString()).isEqualTo("Rui Moreira");
        assertThat(member.get("email").asString()).isEqualTo(ruiUser.email());
        assertThat(member.get("role").asString()).isEqualTo("MEMBER");
        assertThat(dashboardNames(rui)).containsExactly("Website");
    }

    @Test
    void theEmailIsComparedIgnoringCaseAndSpaces() {
        Response added = add(ana, "  " + ruiUser.email().toUpperCase() + " ");                           // AC08, D03

        assertThat(added.status()).isEqualTo(201);
        assertThat(added.json().get("userId").asString()).isEqualTo(ruiUser.id().toString());
    }

    @Test
    void everyMemberSeesTheMembersWithNameEmailAndRole() {
        add(ana, ruiUser.email());

        JsonNode list = api.send("GET", members, null, rui.cookie(), null).json();                      // PRI03

        assertThat(list.valueStream().map(m -> m.get("name").asString() + ":" + m.get("role").asString()))
                .containsExactly("Ana Teixeira:MANAGER", "Rui Moreira:MEMBER");                         // sorted, AS13
        assertThat(list.get(1).get("email").asString()).isEqualTo(ruiUser.email());
        Response outsider = api.send("GET", members, null, sara.cookie(), null);
        assertThat(outsider.status()).isEqualTo(404);
        assertThat(outsider.field("detail")).isEqualTo("Project not found.");
    }

    @Test
    void removeAMember() {
        add(ana, ruiUser.email());

        Response removed = remove(ana, ruiUser.id().toString());                                         // AC02

        assertThat(removed.status()).isEqualTo(204);
        assertThat(dashboardNames(rui)).isEmpty();
        assertThat(api.send("GET", "/api/projects/" + website, null, rui.cookie(), null).field("detail"))
                .isEqualTo("Project not found.");
    }

    @Test
    void theRemovedMemberLeavesTheTasksAssigneesButTeamsStay() {
        add(ana, ruiUser.email());
        add(ana, saraUser.email());
        UUID design = fixtures.createTeam(ana, "Design " + UUID.randomUUID().toString().substring(0, 8));
        fixtures.addTeamMember(ana, design, ruiUser.email());
        UUID createLogo = fixtures.addTask(ana, website, "Create logo", "TODO");
        UUID writeCopy = fixtures.addTask(ana, website, "Write copy", "TODO");
        UUID createIcons = fixtures.addTask(ana, website, "Create icons", "TODO");
        fixtures.assign(ana, website, createLogo, "person", ruiUser.id());
        fixtures.assign(ana, website, writeCopy, "person", ruiUser.id());
        fixtures.assign(ana, website, writeCopy, "person", saraUser.id());
        fixtures.assign(ana, website, createIcons, "team", design);

        remove(ana, ruiUser.id().toString());                                                            // AC03, D10

        assertThat(assigneeNames(createLogo)).isEmpty();
        assertThat(assigneeNames(writeCopy)).containsExactly("Sara Pinto");
        assertThat(assigneeNames(createIcons)).hasSize(1).allSatisfy(name -> assertThat(name).startsWith("Design"));
    }

    @Test
    void anEmailWithoutAnAccountIsRefusedWithoutSayingSo() {
        Response response = add(ana, "nobody-" + UUID.randomUUID() + "@example.com");                    // AC04, SEC09

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.field("detail")).isEqualTo("This user could not be added.");
        assertThat(memberCount()).isEqualTo(1);
    }

    @Test
    void someoneWhoIsAlreadyAMemberIsRefused() {
        add(ana, ruiUser.email());

        Response again = add(ana, ruiUser.email());                                                      // AC05

        assertThat(again.status()).isEqualTo(409);
        assertThat(again.field("detail")).isEqualTo("This user is already a member of the project.");
        assertThat(memberCount()).isEqualTo(2);
    }

    @Test
    void theManagerCantBeRemoved() {
        Response response = remove(ana, fixtures.idOf(ana).toString());                                  // AC06, D07

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.field("detail")).isEqualTo("The project manager cannot be removed.");
        assertThat(memberCount()).isEqualTo(1);
    }

    @Test
    void onlyTheManagerManagesMembers() {
        add(ana, ruiUser.email());

        Response adding = add(rui, saraUser.email());                                                    // AC07
        Response removing = remove(rui, fixtures.idOf(ana).toString());

        assertThat(List.of(adding, removing)).allSatisfy(response -> {
            assertThat(response.status()).isEqualTo(403);
            assertThat(response.field("detail")).isEqualTo("Only the project manager can manage members.");
        });
        assertThat(memberCount()).isEqualTo(2);
        assertThat(add(sara, saraUser.email()).field("detail")).isEqualTo("Project not found.");     // DoD03
    }

    @Test
    void theEmailIsRequired() {
        Map<String, Object> nullEmail = new HashMap<>();
        nullEmail.put("email", null);

        for (String body : List.of("{\"email\":\"\"}", "{\"email\":\"   \"}", "{}", ApiClient.json(nullEmail))) {   // AC10
            Response response = api.send("POST", members, body, ana.cookie(), ana.csrfToken());
            assertThat(response.status()).as(body).isEqualTo(422);
            assertThat(response.field("detail")).as(body).isEqualTo("Email is required.");
        }
        assertThat(memberCount()).isEqualTo(1);
    }

    @Test
    void removingSomeoneWhoIsntAMemberIsRefused() {
        for (String id : List.of(ruiUser.id().toString(), UUID.randomUUID().toString(), "not-an-identifier")) {  // AC11
            Response response = remove(ana, id);
            assertThat(response.status()).as(id).isEqualTo(404);
            assertThat(response.field("detail")).as(id).isEqualTo("Member not found.");
        }
    }

    @Test
    void aMemberRemovedAndAddedAgainDoesntGetTheTasksBack() {
        add(ana, ruiUser.email());
        UUID createLogo = fixtures.addTask(ana, website, "Create logo", "TODO");
        fixtures.assign(ana, website, createLogo, "person", ruiUser.id());
        remove(ana, ruiUser.id().toString());

        assertThat(add(ana, ruiUser.email()).status()).isEqualTo(201);                                   // AC09

        assertThat(dashboardNames(rui)).containsExactly("Website");
        assertThat(assigneeNames(createLogo)).isEmpty();
    }

    @Test
    void changesAreRefusedWithoutTheTokenOrWithAMalformedBody() {
        assertThat(api.send("POST", members, ApiClient.json(Map.of("email", ruiUser.email())), ana.cookie(), null)
                .status()).isEqualTo(403);                                                               // DoD06
        assertThat(api.send("DELETE", members + "/" + fixtures.idOf(ana), null, ana.cookie(), null).status())
                .isEqualTo(403);
        assertThat(api.send("POST", members, "{\"email\":\"x@example.com\",\"role\":\"MANAGER\"}", ana.cookie(),
                ana.csrfToken()).status()).isEqualTo(400);                                               // DoD07
        assertThat(api.send("GET", members, null, null, null).status()).isEqualTo(401);                  // DoD02
        assertThat(memberCount()).isEqualTo(1);
    }

    /**
     * PD05 (US10 AC05): an assignment holds the person's membership while it is saved; a removal made at the same
     * time waits for it and then removes the new assignment too, so no assignment survives a removal.
     */
    @Test
    void anAssignmentAndARemovalAtTheSameTimeLeaveNoAssignmentBehind() throws Exception {
        add(ana, ruiUser.email());
        UUID task = fixtures.addTask(ana, website, "Create logo", "TODO");
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch removalStarted = new CountDownLatch(1);

        CompletableFuture<Void> assignment = CompletableFuture.runAsync(() -> transactions.executeWithoutResult(status -> {
            // What addTaskAssignee does inside its transaction: check and lock the membership, then save.
            assertThat(projectMembers.lockIfMember(website, ruiUser.id())).isTrue();
            locked.countDown();
            await(removalStarted);
            sleep(500);   // the removal is now waiting for this transaction
            jdbc.sql("insert into task_assignee_user (task_id, user_id) values (?, ?)").params(task, ruiUser.id()).update();
        }));
        await(locked);
        CompletableFuture<Response> removal = CompletableFuture.supplyAsync(() -> {
            removalStarted.countDown();
            return remove(ana, ruiUser.id().toString());
        });

        assignment.get(10, TimeUnit.SECONDS);
        assertThat(removal.get(10, TimeUnit.SECONDS).status()).isEqualTo(204);
        assertThat(fixtures.count("task_assignee_user", "task_id", task)).isZero();

        // And the other way round: once removed, an assignment is refused.
        Response late = fixtures.assign(ana, website, task, "person", ruiUser.id());
        assertThat(late.status()).isEqualTo(422);
        assertThat(late.field("detail")).isEqualTo("The assignee must be a member of the project.");
    }

    // ------------------------------------------------------------------ helpers

    private Response add(LoggedIn user, String email) {
        return api.send("POST", members, ApiClient.json(Map.of("email", email)), user.cookie(), user.csrfToken());
    }

    private Response remove(LoggedIn user, String userId) {
        return api.send("DELETE", members + "/" + userId, null, user.cookie(), user.csrfToken());
    }

    private int memberCount() {
        return fixtures.count("project_member", "project_id", website);
    }

    private List<String> dashboardNames(LoggedIn user) {
        return api.send("GET", "/api/projects", null, user.cookie(), null).json().get("projects")
                .valueStream().map(project -> project.get("name").asString()).toList();
    }

    private List<String> assigneeNames(UUID task) {
        return api.send("GET", "/api/projects/" + website + "/tasks/" + task, null, ana.cookie(), null).json()
                .get("assignees").valueStream().map(assignee -> assignee.get("name").asString()).toList();
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
