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
 * Increment I6: a task's comments (US21, US22; D13, SEC11), through real HTTP requests. Ana is the project
 * manager, Rui a member, Marta not a member.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import(TestcontainersConfiguration.class)
class CommentsApiTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PasswordEncoder encoder;

    ApiClient api;
    ProjectFixtures fixtures;
    TestUser ruiUser;
    LoggedIn ana;
    LoggedIn rui;
    LoggedIn marta;
    UUID website;
    UUID createLogo;
    String comments;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
        fixtures = new ProjectFixtures(api, jdbc);
        TestUser anaUser = TestUsers.create(jdbc, encoder, "Ana Silva");
        ruiUser = TestUsers.create(jdbc, encoder, "Rui Costa");
        TestUser martaUser = TestUsers.create(jdbc, encoder, "Marta Reis");
        ana = api.logInAs(anaUser.email(), TestUsers.PASSWORD);
        rui = api.logInAs(ruiUser.email(), TestUsers.PASSWORD);
        marta = api.logInAs(martaUser.email(), TestUsers.PASSWORD);
        website = fixtures.createProject(ana, "Website");
        fixtures.addMember(ana, website, ruiUser.email());
        createLogo = fixtures.addTask(ana, website, "Create logo", "TODO");
        comments = "/api/projects/" + website + "/tasks/" + createLogo + "/comments";
    }

    // ------------------------------------------------------------------ US21

    @Test
    void addACommentAndListThemOldestFirst() {
        Response added = add(ana, "First draft is ready");                                               // AC01

        assertThat(added.status()).isEqualTo(201);
        JsonNode comment = added.json();
        assertThat(comment.get("text").asString()).isEqualTo("First draft is ready");
        assertThat(comment.get("author").get("name").asString()).isEqualTo("Ana Silva");
        assertThat(comment.get("createdAt").asString()).isNotBlank();
        assertThat(comment.get("editedAt").isNull()).isTrue();

        add(rui, "Looks good");                                                                          // AC02
        assertThat(texts(list(rui))).containsExactly("First draft is ready", "Looks good");
    }

    @Test
    void theTextIsRequired() {
        Map<String, Object> nullText = new HashMap<>();
        nullText.put("text", null);
        for (String body : List.of("{\"text\":\"\"}", "{\"text\":\"   \"}", "{}", ApiClient.json(nullText))) {   // AC03
            Response response = api.send("POST", comments, body, ana.cookie(), ana.csrfToken());
            assertThat(response.status()).as(body).isEqualTo(422);
            assertThat(response.field("detail")).isEqualTo("Comment is required.");
        }
        assertThat(list(ana).json().isEmpty()).isTrue();                                                // AC07
    }

    @Test
    void aNonMemberCantReadOrAddComments() {
        add(ana, "First draft is ready");

        Response reading = api.send("GET", comments, null, marta.cookie(), null);                       // AC04
        Response adding = add(marta, "Hello");

        assertThat(List.of(reading, adding)).allSatisfy(response -> {
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.field("detail")).isEqualTo("Project not found.");
        });
        assertThat(reading.body()).doesNotContain("First draft");
        assertThat(texts(list(ana))).containsExactly("First draft is ready");
    }

    @Test
    void textLimits() {
        assertThat(add(ana, "c".repeat(2000)).status()).isEqualTo(201);                                  // AC05
        Response tooLong = add(ana, "c".repeat(2001));
        assertThat(tooLong.status()).isEqualTo(422);
        assertThat(tooLong.field("detail")).isEqualTo("Comment must be at most 2000 characters.");
    }

    @Test
    void aCommentStaysWithItsAuthorsNameWhenTheAuthorLeaves() {
        add(rui, "Looks good");

        api.send("DELETE", "/api/projects/" + website + "/members/" + ruiUser.id(), null, ana.cookie(), ana.csrfToken());

        JsonNode comment = list(ana).json().get(0);                                                      // AC06, D13
        assertThat(comment.get("text").asString()).isEqualTo("Looks good");
        assertThat(comment.get("author").get("name").asString()).isEqualTo("Rui Costa");
    }

    // ------------------------------------------------------------------ US22

    @Test
    void editMyComment() {
        String id = add(rui, "Looks good").json().get("id").asString();

        Response edited = edit(rui, id, "Looks good, but the icon is too small");                        // AC01

        assertThat(edited.status()).isEqualTo(200);
        assertThat(edited.json().get("text").asString()).isEqualTo("Looks good, but the icon is too small");
        assertThat(edited.json().get("editedAt").isNull()).isFalse();
    }

    @Test
    void deleteMyComment() {
        String id = add(rui, "Looks good").json().get("id").asString();

        Response deleted = api.send("DELETE", comments + "/" + id, null, rui.cookie(), rui.csrfToken());  // AC03

        assertThat(deleted.status()).isEqualTo(204);
        assertThat(list(ana).json().isEmpty()).isTrue();
    }

    @Test
    void onlyTheAuthorCanChangeAComment() {
        String id = add(rui, "Looks good").json().get("id").asString();

        Response editing = edit(ana, id, "Ana's text");                                                  // AC05, SEC11
        Response deleting = api.send("DELETE", comments + "/" + id, null, ana.cookie(), ana.csrfToken());

        assertThat(List.of(editing, deleting)).allSatisfy(response -> {
            assertThat(response.status()).isEqualTo(403);
            assertThat(response.field("detail")).isEqualTo("Only the author can change this comment.");
        });
        assertThat(texts(list(rui))).containsExactly("Looks good");
    }

    @Test
    void aClearedTextIsRefused() {
        String id = add(rui, "Looks good").json().get("id").asString();

        Response response = edit(rui, id, "");                                                           // AC06

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.field("detail")).isEqualTo("Comment is required.");
        assertThat(texts(list(rui))).containsExactly("Looks good");
    }

    @Test
    void aCommentDeletedInTheMeantimeIsNotFound() {
        String id = add(rui, "Looks good").json().get("id").asString();
        api.send("DELETE", comments + "/" + id, null, rui.cookie(), rui.csrfToken());
        UUID otherTask = fixtures.addTask(ana, website, "Create icons", "TODO");
        String otherTasksComment = api.send("POST", "/api/projects/" + website + "/tasks/" + otherTask + "/comments",
                ApiClient.json(Map.of("text", "Elsewhere")), rui.cookie(), rui.csrfToken()).json().get("id").asString();

        List<Response> responses = List.of(                                                              // AC07
                edit(rui, id, "Again"),
                api.send("DELETE", comments + "/" + id, null, rui.cookie(), rui.csrfToken()),
                edit(rui, otherTasksComment, "Moved"),
                edit(rui, "not-an-identifier", "Again"));

        assertThat(responses).allSatisfy(response -> {
            assertThat(response.status()).isEqualTo(404);
            assertThat(response.field("detail")).isEqualTo("Comment not found.");
        });
    }

    @Test
    void savingWithoutChangesDoesntMarkTheCommentAsEdited() {
        String id = add(rui, "Looks good").json().get("id").asString();

        Response saved = edit(rui, id, "  Looks good  ");                                                // AC08, D01

        assertThat(saved.status()).isEqualTo(200);
        assertThat(saved.json().get("editedAt").isNull()).isTrue();
    }

    @Test
    void commentsNeedASessionTheTokenAndAWellFormedBody() {
        String id = add(rui, "Looks good").json().get("id").asString();

        assertThat(api.send("GET", comments, null, null, null).status()).isEqualTo(401);                // DoD02
        assertThat(api.send("POST", comments, "{\"text\":\"x\"}", rui.cookie(), null).status()).isEqualTo(403);   // DoD06
        assertThat(api.send("PATCH", comments + "/" + id, "{\"text\":\"x\"}", rui.cookie(), null).status()).isEqualTo(403);
        assertThat(api.send("POST", comments, "{\"text\":\"x\",\"authorId\":\"" + UUID.randomUUID() + "\"}",
                rui.cookie(), rui.csrfToken()).status()).isEqualTo(400);                                // DoD07
        assertThat(texts(list(rui))).containsExactly("Looks good");
    }

    @Test
    void deletingTheTaskDeletesItsComments() {
        add(rui, "Looks good");

        api.send("DELETE", "/api/projects/" + website + "/tasks/" + createLogo, null, ana.cookie(), ana.csrfToken());

        assertThat(fixtures.count("task_comment", "task_id", createLogo)).isZero();                     // R22
        assertThat(list(ana).field("detail")).isEqualTo("Task not found.");
    }

    // ------------------------------------------------------------------ helpers

    private Response add(LoggedIn user, String text) {
        return api.send("POST", comments, ApiClient.json(Map.of("text", text)), user.cookie(), user.csrfToken());
    }

    private Response edit(LoggedIn user, String id, String text) {
        return api.send("PATCH", comments + "/" + id, ApiClient.json(Map.of("text", text)), user.cookie(), user.csrfToken());
    }

    private Response list(LoggedIn user) {
        return api.send("GET", comments, null, user.cookie(), null);
    }

    private static List<String> texts(Response list) {
        return list.json().valueStream().map(comment -> comment.get("text").asString()).toList();
    }
}
