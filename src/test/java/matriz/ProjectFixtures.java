package matriz;

import java.util.Map;
import java.util.UUID;
import matriz.ApiClient.LoggedIn;
import matriz.ApiClient.Response;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Test data, with made-up content (PRI05). Projects and tasks are created through the API; memberships
 * (US09, I4), assignees and comments have no API operation yet, so they are written straight into the tables.
 */
final class ProjectFixtures {

    private final ApiClient api;
    private final JdbcClient jdbc;

    ProjectFixtures(ApiClient api, JdbcClient jdbc) {
        this.api = api;
        this.jdbc = jdbc;
    }

    /** Creates a project through the API, as the logged-in user, and returns its identifier. */
    UUID createProject(LoggedIn user, String name) {
        Response created = api.send("POST", "/api/projects", ApiClient.json(Map.of("name", name)),
                user.cookie(), user.csrfToken());
        if (created.status() != 201) {
            throw new IllegalStateException("Project not created: " + created.status() + " " + created.body());
        }
        return UUID.fromString(created.json().get("id").asString());
    }

    void addMember(UUID projectId, UUID userId) {
        jdbc.sql("insert into project_member (project_id, user_id) values (?, ?)").params(projectId, userId).update();
    }

    void removeMember(UUID projectId, UUID userId) {
        jdbc.sql("delete from project_member where project_id = ? and user_id = ?").params(projectId, userId).update();
    }

    /** Adds {@code count} tasks with the given status ({@code TODO}, {@code IN_PROGRESS} or {@code DONE}). */
    void addTasks(LoggedIn user, UUID projectId, String status, int count) {
        for (int i = 0; i < count; i++) {
            addTask(user, projectId, "Task " + i, status);
        }
    }

    /** Creates a task through the API, as the logged-in member, and moves it to {@code status}. */
    UUID addTask(LoggedIn user, UUID projectId, String title, String status) {
        Response created = api.send("POST", "/api/projects/" + projectId + "/tasks",
                ApiClient.json(Map.of("title", title)), user.cookie(), user.csrfToken());
        if (created.status() != 201) {
            throw new IllegalStateException("Task not created: " + created.status() + " " + created.body());
        }
        UUID id = UUID.fromString(created.json().get("id").asString());
        if (!"TODO".equals(status)) {
            setStatus(user, projectId, id, status);
        }
        return id;
    }

    /** Changes a task's status through the API (US07). */
    void setStatus(LoggedIn user, UUID projectId, UUID taskId, String status) {
        Response moved = api.send("PATCH", "/api/projects/" + projectId + "/tasks/" + taskId,
                ApiClient.json(Map.of("status", status)), user.cookie(), user.csrfToken());
        if (moved.status() != 200) {
            throw new IllegalStateException("Status not changed: " + moved.status() + " " + moved.body());
        }
    }

    /** Adds a member through the API (US09), as the project's manager. */
    void addMember(LoggedIn manager, UUID projectId, String email) {
        expect(201, api.send("POST", "/api/projects/" + projectId + "/members",
                ApiClient.json(Map.of("email", email)), manager.cookie(), manager.csrfToken()));
    }

    /** Creates a team through the API (US11) and returns its identifier. */
    UUID createTeam(LoggedIn user, String name) {
        Response created = expect(201, api.send("POST", "/api/teams", ApiClient.json(Map.of("name", name)),
                user.cookie(), user.csrfToken()));
        return UUID.fromString(created.json().get("id").asString());
    }

    /** Adds a team member through the API (US12), as the team's manager. */
    void addTeamMember(LoggedIn manager, UUID teamId, String email) {
        expect(201, api.send("POST", "/api/teams/" + teamId + "/members", ApiClient.json(Map.of("email", email)),
                manager.cookie(), manager.csrfToken()));
    }

    /** Assigns a person ({@code "person"}) or a team ({@code "team"}) through the API (US10, US13). */
    Response assign(LoggedIn member, UUID projectId, UUID taskId, String type, UUID id) {
        String key = "person".equals(type) ? "userId" : "teamId";
        return api.send("POST", "/api/projects/" + projectId + "/tasks/" + taskId + "/assignees",
                ApiClient.json(Map.of("type", type, key, id.toString())), member.cookie(), member.csrfToken());
    }

    /** The logged-in user's identifier. */
    UUID idOf(LoggedIn user) {
        return UUID.fromString(api.send("GET", "/api/session", null, user.cookie(), null)
                .json().get("user").get("id").asString());
    }

    private static Response expect(int status, Response response) {
        if (response.status() != status) {
            throw new IllegalStateException("Expected " + status + ", got " + response.status() + " " + response.body());
        }
        return response;
    }

    void assignUser(UUID taskId, UUID userId) {
        jdbc.sql("insert into task_assignee_user (task_id, user_id) values (?, ?)").params(taskId, userId).update();
    }

    /** A team with one member, assigned to the task. Returns the team's identifier. */
    UUID assignNewTeam(UUID taskId, UUID managerId) {
        UUID team = UUID.randomUUID();
        jdbc.sql("insert into team (id, name, manager_id) values (?, ?, ?)")
                .params(team, "Design " + team, managerId)
                .update();
        jdbc.sql("insert into team_member (team_id, user_id) values (?, ?)").params(team, managerId).update();
        jdbc.sql("insert into task_assignee_team (task_id, team_id) values (?, ?)").params(taskId, team).update();
        return team;
    }

    void addComment(UUID taskId, UUID authorId, String text) {
        jdbc.sql("insert into task_comment (id, task_id, author_id, text) values (?, ?, ?, ?)")
                .params(UUID.randomUUID(), taskId, authorId, text)
                .update();
    }

    int count(String table, String column, UUID value) {
        return jdbc.sql("select count(*) from " + table + " where " + column + " = ?")
                .param(value)
                .query(Integer.class)
                .single();
    }
}
