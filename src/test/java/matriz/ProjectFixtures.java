package matriz;

import java.util.Map;
import java.util.UUID;
import matriz.ApiClient.LoggedIn;
import matriz.ApiClient.Response;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Data the project tests need but that no API operation creates yet: memberships (US09, I5), tasks (US05, I3),
 * assignees and comments. Written straight into the tables, with made-up content (PRI05).
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

    /** Adds {@code count} tasks with the given status ({@code TO_DO}, {@code IN_PROGRESS} or {@code DONE}). */
    void addTasks(UUID projectId, String status, int count) {
        for (int i = 0; i < count; i++) {
            addTask(projectId, "Task " + i, status);
        }
    }

    UUID addTask(UUID projectId, String title, String status) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into task (id, project_id, title, status) values (?, ?, ?, ?)")
                .params(id, projectId, title, status)
                .update();
        return id;
    }

    void setStatus(UUID taskId, String status) {
        jdbc.sql("update task set status = ? where id = ?").params(status, taskId).update();
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
