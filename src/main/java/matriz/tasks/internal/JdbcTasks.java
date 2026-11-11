package matriz.tasks.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Tasks in the database (docs/database.md: TASK). */
@Component
class JdbcTasks {

    /** A task as the API returns it. {@link TaskAssignees} fills in the assignees (US06, US10, US13). */
    record TaskView(UUID id, UUID projectId, String title, String description, LocalDate dueDate, String status,
            String priority, List<AssigneeView> assignees, OffsetDateTime createdAt) {

        TaskView withAssignees(List<AssigneeView> list) {
            return new TaskView(id, projectId, title, description, dueDate, status, priority, list, createdAt);
        }
    }

    /** A person ({@code userId}) or a team ({@code teamId}) responsible for a task, with the name shown. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record AssigneeView(String type, UUID userId, UUID teamId, String name) {
    }

    record Fields(String title, String description, LocalDate dueDate, String status, String priority) {
    }

    private static final String COLUMNS = "id, project_id, title, description, due_date, status, priority, created_at";

    private final JdbcClient jdbc;

    JdbcTasks(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** A new task always starts in {@code TODO} (US05). */
    UUID create(UUID projectId, String title, String description, LocalDate dueDate, String priority) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                insert into task (id, project_id, title, description, due_date, status, priority)
                values (?, ?, ?, ?, ?, 'TODO', ?)
                """)
                .params(id, projectId, title, description, dueDate, priority)
                .update();
        return id;
    }

    /**
     * The project's tasks, newest first (US06 AC06). A task is kept if its status is one of {@code statuses}
     * <strong>and</strong> its priority one of {@code priorities}; an empty list doesn't filter (US24).
     */
    List<TaskView> list(UUID projectId, List<String> statuses, List<String> priorities) {
        StringBuilder sql = new StringBuilder("select " + COLUMNS + " from task where project_id = :project");
        if (!statuses.isEmpty()) {
            sql.append(" and status in (:statuses)");
        }
        if (!priorities.isEmpty()) {
            sql.append(" and priority in (:priorities)");
        }
        sql.append(" order by created_at desc, id");

        JdbcClient.StatementSpec statement = jdbc.sql(sql.toString()).param("project", projectId);
        if (!statuses.isEmpty()) {
            statement = statement.param("statuses", statuses);
        }
        if (!priorities.isEmpty()) {
            statement = statement.param("priorities", priorities);
        }
        return statement.query((row, number) -> view(row)).list();
    }

    /** The task, only if it belongs to that project (DoD05). */
    Optional<TaskView> find(UUID projectId, UUID taskId) {
        return jdbc.sql("select " + COLUMNS + " from task where id = ? and project_id = ?")
                .params(taskId, projectId)
                .query((row, number) -> view(row))
                .optional();
    }

    /** Saves every field at once: the last saved change wins (D11). Returns false if the task is gone. */
    boolean update(UUID projectId, UUID taskId, Fields fields) {
        return jdbc.sql("""
                update task set title = ?, description = ?, due_date = ?, status = ?, priority = ?
                where id = ? and project_id = ?
                """)
                .params(fields.title(), fields.description(), fields.dueDate(), fields.status(), fields.priority(),
                        taskId, projectId)
                .update() == 1;
    }

    /** Deletes the task; its assignees and comments go with it (R22, cascade). Returns false if it was gone. */
    boolean delete(UUID projectId, UUID taskId) {
        return jdbc.sql("delete from task where id = ? and project_id = ?").params(taskId, projectId).update() == 1;
    }

    private static TaskView view(ResultSet row) throws SQLException {
        return new TaskView(row.getObject("id", UUID.class), row.getObject("project_id", UUID.class),
                row.getString("title"), row.getString("description"), row.getObject("due_date", LocalDate.class),
                row.getString("status"), row.getString("priority"), List.of(),
                row.getObject("created_at", OffsetDateTime.class));
    }
}
