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
 * Increment I5: teams (US11, US12, US17, US18; D05, D06, D07, D09, PD11), through real HTTP requests. Team names
 * are unique across the whole database, so each test gives its teams a name of its own.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import(TestcontainersConfiguration.class)
class TeamsApiTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    PasswordEncoder encoder;

    ApiClient api;
    ProjectFixtures fixtures;
    TestUser ruiUser;
    TestUser martaUser;
    LoggedIn ana;
    LoggedIn rui;
    LoggedIn marta;
    String design;

    @BeforeEach
    void setUp() {
        api = new ApiClient(port);
        fixtures = new ProjectFixtures(api, jdbc);
        TestUser anaUser = TestUsers.create(jdbc, encoder, "Ana Teixeira");
        ruiUser = TestUsers.create(jdbc, encoder, "Rui Moreira");
        martaUser = TestUsers.create(jdbc, encoder, "Marta Reis");
        ana = api.logInAs(anaUser.email(), TestUsers.PASSWORD);
        rui = api.logInAs(ruiUser.email(), TestUsers.PASSWORD);
        marta = api.logInAs(martaUser.email(), TestUsers.PASSWORD);
        design = unique("Design");
    }

    // ------------------------------------------------------------------ US11 — create

    @Test
    void createATeamThatEveryUserSees() {
        Response created = create(ana, design);                                                          // AC01

        assertThat(created.status()).isEqualTo(201);
        JsonNode team = created.json();
        assertThat(team.get("name").asString()).isEqualTo(design);
        assertThat(created.location()).isEqualTo("/api/teams/" + team.get("id").asString());
        assertThat(members(team)).containsExactly("Ana Teixeira:MANAGER");                               // D07
        JsonNode seenByMarta = teamIn(list(marta), team.get("id").asString());                           // D05, PD11
        assertThat(members(seenByMarta)).containsExactly("Ana Teixeira:MANAGER");
        assertThat(list(marta).body()).doesNotContain(ruiUser.email());                                  // PRI04: no emails
    }

    @Test
    void theNameIsRequired() {
        Map<String, Object> nullName = new HashMap<>();
        nullName.put("name", null);
        for (String body : List.of("{\"name\":\"\"}", "{\"name\":\"   \"}", "{}", ApiClient.json(nullName))) {   // AC02
            Response response = api.send("POST", "/api/teams", body, ana.cookie(), ana.csrfToken());
            assertThat(response.status()).as(body).isEqualTo(422);
            assertThat(response.field("detail")).as(body).isEqualTo("Team name is required.");
        }
    }

    @Test
    void aNameAlreadyInUseIsRefusedIgnoringCaseAndSpaces() {
        create(ana, design);

        for (String name : List.of(design, "  " + design.toUpperCase() + "  ")) {                       // AC03, AC04, D06
            Response response = create(rui, name);
            assertThat(response.status()).as(name).isEqualTo(409);
            assertThat(response.field("detail")).as(name).isEqualTo("A team with this name already exists.");
        }
    }

    @Test
    void nameLimits() {
        String fifty = unique("T").concat("x".repeat(50)).substring(0, 50);

        // One character no other test uses: a random letter from a block of 20 000 CJK characters.
        String one = String.valueOf((char) (0x4E00 + new java.util.Random().nextInt(20_000)));
        assertThat(create(ana, one).status()).isEqualTo(201);                                            // AC05
        assertThat(create(ana, fifty).status()).isEqualTo(201);
        Response tooLong = create(ana, fifty + "y");
        assertThat(tooLong.status()).isEqualTo(422);
        assertThat(tooLong.field("detail")).isEqualTo("Team name must be at most 50 characters.");
    }

    @Test
    void theListIsSortedByNameWithItsMembers() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        create(ana, "beta " + suffix);
        create(ana, "Alpha " + suffix);

        List<String> names = list(ana).json().valueStream().map(team -> team.get("name").asString())
                .filter(name -> name.endsWith(suffix)).toList();

        assertThat(names).containsExactly("Alpha " + suffix, "beta " + suffix);
    }

    // ------------------------------------------------------------------ US12 — members

    @Test
    void addAndRemoveAMember() {
        UUID team = fixtures.createTeam(ana, design);

        Response added = addMember(ana, team, ruiUser.email().toUpperCase());                            // AC01, AC07

        assertThat(added.status()).isEqualTo(201);
        assertThat(added.json().get("role").asString()).isEqualTo("MEMBER");
        assertThat(members(teamIn(list(marta), team.toString()))).containsExactly("Ana Teixeira:MANAGER", "Rui Moreira:MEMBER");

        assertThat(removeMember(ana, team, ruiUser.id().toString()).status()).isEqualTo(204);           // AC02
        assertThat(members(teamIn(list(ana), team.toString()))).containsExactly("Ana Teixeira:MANAGER");
    }

    @Test
    void memberErrors() {
        UUID team = fixtures.createTeam(ana, design);
        addMember(ana, team, ruiUser.email());

        assertProblem(addMember(ana, team, "nobody-" + UUID.randomUUID() + "@example.com"), 422,
                "This user could not be added.");                                                       // AC03, SEC09
        assertProblem(addMember(ana, team, ruiUser.email()), 409, "This user is already a member of the team."); // AC04
        assertProblem(removeMember(ana, team, fixtures.idOf(ana).toString()), 422, "The team manager cannot be removed."); // AC05
        assertProblem(addMember(rui, team, martaUser.email()), 403, "Only the team manager can manage members."); // AC06
        assertProblem(removeMember(rui, team, ruiUser.id().toString()), 403, "Only the team manager can manage members.");
        assertProblem(addMember(marta, team, martaUser.email()), 403, "Only the team manager can manage members.");
        assertProblem(addMember(ana, team, "  "), 422, "Email is required.");                            // AC09
        assertProblem(removeMember(ana, team, martaUser.id().toString()), 404, "Member not found.");     // AC10
        assertProblem(removeMember(ana, team, "not-an-identifier"), 404, "Member not found.");
        assertProblem(addMember(ana, UUID.randomUUID(), ruiUser.email()), 404, "Team not found.");      // AC11
        assertProblem(removeMember(ana, UUID.randomUUID(), ruiUser.id().toString()), 404, "Team not found.");
        assertThat(members(teamIn(list(ana), team.toString()))).containsExactly("Ana Teixeira:MANAGER", "Rui Moreira:MEMBER");
    }

    @Test
    void changingTheTeamDoesntUndoAssignmentsOrGiveProjectAccess() {
        UUID website = fixtures.createProject(ana, "Website");
        UUID team = fixtures.createTeam(ana, design);
        UUID createIcons = fixtures.addTask(ana, website, "Create icons", "TODO");
        fixtures.assign(ana, website, createIcons, "team", team);

        addMember(ana, team, martaUser.email());                                                         // AC08, D09

        assertThat(api.send("GET", "/api/projects/" + website + "/tasks/" + createIcons, null, ana.cookie(), null)
                .json().get("assignees").get(0).get("name").asString()).isEqualTo(design);
        assertThat(api.send("GET", "/api/projects/" + website, null, marta.cookie(), null).status()).isEqualTo(404);
    }

    // ------------------------------------------------------------------ US17 — rename

    @Test
    void renameATeamAndItsTasksShowTheNewName() {
        UUID website = fixtures.createProject(ana, "Website");
        UUID team = fixtures.createTeam(ana, design);
        UUID createLogo = fixtures.addTask(ana, website, "Create logo", "TODO");
        fixtures.assign(ana, website, createLogo, "team", team);
        String productDesign = unique("Product design");

        Response renamed = rename(ana, team, productDesign);                                             // AC01

        assertThat(renamed.status()).isEqualTo(200);
        assertThat(renamed.json().get("name").asString()).isEqualTo(productDesign);
        assertThat(api.send("GET", "/api/projects/" + website + "/tasks/" + createLogo, null, ana.cookie(), null)
                .json().get("assignees").get(0).get("name").asString()).isEqualTo(productDesign);
    }

    @Test
    void renameErrors() {
        UUID team = fixtures.createTeam(ana, design);
        String qa = unique("QA");
        fixtures.createTeam(rui, qa);

        assertProblem(rename(ana, team, qa.toLowerCase()), 409, "A team with this name already exists.");       // AC02
        assertProblem(rename(ana, team, ""), 422, "Team name is required.");                                  // AC03
        assertProblem(rename(rui, team, unique("Rui's")), 403, "Only the team manager can edit the team.");   // AC04
        assertProblem(rename(ana, UUID.randomUUID(), unique("X")), 404, "Team not found.");                   // AC05
        assertProblem(rename(ana, team, "x".repeat(51)), 422, "Team name must be at most 50 characters.");    // AC07
        assertThat(teamIn(list(ana), team.toString()).get("name").asString()).isEqualTo(design);
    }

    @Test
    void changeOnlyTheCasingOfTheTeamsOwnName() {
        UUID team = fixtures.createTeam(ana, design);

        Response renamed = rename(ana, team, design.toUpperCase());                                     // AC06

        assertThat(renamed.status()).isEqualTo(200);
        assertThat(renamed.json().get("name").asString()).isEqualTo(design.toUpperCase());
    }

    // ------------------------------------------------------------------ US18 — delete

    @Test
    void deletingATeamRemovesItFromItsTasksOnly() {
        UUID website = fixtures.createProject(ana, "Website");
        fixtures.addMember(ana, website, ruiUser.email());
        fixtures.addMember(ana, website, martaUser.email());
        UUID team = fixtures.createTeam(ana, design);
        addMember(ana, team, ruiUser.email());
        UUID createLogo = fixtures.addTask(ana, website, "Create logo", "TODO");
        UUID createIcons = fixtures.addTask(ana, website, "Create icons", "TODO");
        fixtures.assign(ana, website, createLogo, "team", team);
        fixtures.assign(ana, website, createIcons, "team", team);
        fixtures.assign(ana, website, createIcons, "person", martaUser.id());

        Response deleted = api.send("DELETE", "/api/teams/" + team, null, ana.cookie(), ana.csrfToken());  // AC02

        assertThat(deleted.status()).isEqualTo(204);
        assertThat(teamIds(list(ana))).doesNotContain(team.toString());
        assertThat(assignees(ana, website, createLogo)).isEmpty();
        assertThat(assignees(ana, website, createIcons)).containsExactly("Marta Reis");
        assertThat(api.send("GET", "/api/projects/" + website, null, rui.cookie(), null).status()).isEqualTo(200);
        assertThat(fixtures.count("team_member", "team_id", team)).isZero();
    }

    @Test
    void deleteErrorsAndEdgeCases() {
        UUID team = fixtures.createTeam(ana, design);
        addMember(ana, team, ruiUser.email());

        assertProblem(api.send("DELETE", "/api/teams/" + team, null, rui.cookie(), rui.csrfToken()), 403,
                "Only the team manager can delete the team.");                                          // AC04
        assertProblem(api.send("DELETE", "/api/teams/" + UUID.randomUUID(), null, ana.cookie(), ana.csrfToken()), 404,
                "Team not found.");                                                                     // AC05
        assertThat(api.send("DELETE", "/api/teams/" + team, null, ana.cookie(), null).status()).isEqualTo(403);   // DoD06
        assertThat(teamIds(list(ana))).contains(team.toString());

        assertThat(api.send("DELETE", "/api/teams/" + team, null, ana.cookie(), ana.csrfToken()).status()).isEqualTo(204); // AC06
        assertThat(create(marta, design).status()).isEqualTo(201);                                      // AC07
    }

    @Test
    void teamsNeedASessionAndAWellFormedBody() {
        assertThat(api.send("GET", "/api/teams", null, null, null).status()).isEqualTo(401);             // DoD02
        assertThat(api.send("POST", "/api/teams", ApiClient.json(Map.of("name", design)), ana.cookie(), null)
                .status()).isEqualTo(403);                                                              // DoD06
        assertThat(api.send("POST", "/api/teams", "{\"name\":\"" + design + "\",\"managerId\":\"x\"}", ana.cookie(),
                ana.csrfToken()).status()).isEqualTo(400);                                              // DoD07
        assertThat(api.send("POST", "/api/teams", "{\"name\":7}", ana.cookie(), ana.csrfToken()).status()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ helpers

    private static String unique(String name) {
        return name + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private Response create(LoggedIn user, String name) {
        return api.send("POST", "/api/teams", ApiClient.json(Map.of("name", name)), user.cookie(), user.csrfToken());
    }

    private Response rename(LoggedIn user, UUID team, String name) {
        return api.send("PATCH", "/api/teams/" + team, ApiClient.json(Map.of("name", name)), user.cookie(), user.csrfToken());
    }

    private Response addMember(LoggedIn user, UUID team, String email) {
        return api.send("POST", "/api/teams/" + team + "/members", ApiClient.json(Map.of("email", email)),
                user.cookie(), user.csrfToken());
    }

    private Response removeMember(LoggedIn user, UUID team, String userId) {
        return api.send("DELETE", "/api/teams/" + team + "/members/" + userId, null, user.cookie(), user.csrfToken());
    }

    private Response list(LoggedIn user) {
        return api.send("GET", "/api/teams", null, user.cookie(), null);
    }

    private static JsonNode teamIn(Response list, String teamId) {
        return list.json().valueStream().filter(team -> team.get("id").asString().equals(teamId)).findFirst().orElseThrow();
    }

    private static List<String> teamIds(Response list) {
        return list.json().valueStream().map(team -> team.get("id").asString()).toList();
    }

    private static List<String> members(JsonNode team) {
        return team.get("members").valueStream()
                .map(member -> member.get("name").asString() + ":" + member.get("role").asString()).toList();
    }

    private List<String> assignees(LoggedIn user, UUID project, UUID task) {
        return api.send("GET", "/api/projects/" + project + "/tasks/" + task, null, user.cookie(), null).json()
                .get("assignees").valueStream().map(assignee -> assignee.get("name").asString()).toList();
    }

    private static void assertProblem(Response response, int status, String detail) {
        assertThat(response.status()).as(detail).isEqualTo(status);
        assertThat(response.field("detail")).isEqualTo(detail);
    }
}
