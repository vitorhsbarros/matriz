package matriz.tasks.internal;

import matriz.projects.ProjectDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Deletes a deleted project's tasks (R17, FR05, US15 AC02). Their assignees and comments go with them
 * (cascade inside Tasks, R22); teams are not touched (US15 AC06).
 *
 * <p>A plain, synchronous {@code @EventListener}: it runs in the transaction that deletes the project, before
 * the project's row is deleted, so a failure here rolls the whole deletion back
 * (04-solution-architecture.md, section 3.2, rule 4).
 */
@Component
class ProjectDeletedHandler {

    private final JdbcClient jdbc;

    ProjectDeletedHandler(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener
    void on(ProjectDeleted event) {
        jdbc.sql("delete from task where project_id = ?").param(event.projectId()).update();
    }
}
