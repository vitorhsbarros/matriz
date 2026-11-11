package matriz.tasks.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Number of tasks per status (FR07): plain counts, not statistics (C02). */
@Component
class JdbcTaskCounts {

    record TaskCounts(int toDo, int inProgress, int done) {

        static final TaskCounts NONE = new TaskCounts(0, 0, 0);

        TaskCounts plus(TaskCounts other) {
            return new TaskCounts(toDo + other.toDo, inProgress + other.inProgress, done + other.done);
        }
    }

    private final JdbcClient jdbc;

    JdbcTaskCounts(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** The counts of each given project; a project without tasks gets zeros (US04 AC08). */
    Map<UUID, TaskCounts> perProject(List<UUID> projectIds) {
        Map<UUID, TaskCounts> counts = new HashMap<>();
        projectIds.forEach(id -> counts.put(id, TaskCounts.NONE));
        if (projectIds.isEmpty()) {
            return counts;
        }
        jdbc.sql("""
                select project_id,
                       count(*) filter (where status = 'TODO')       as to_do,
                       count(*) filter (where status = 'IN_PROGRESS') as in_progress,
                       count(*) filter (where status = 'DONE')        as done
                from task where project_id in (:projects)
                group by project_id
                """)
                .param("projects", projectIds)
                .query((row, number) -> {
                    counts.put(row.getObject("project_id", UUID.class),
                            new TaskCounts(row.getInt("to_do"), row.getInt("in_progress"), row.getInt("done")));
                    return null;
                })
                .list();
        return counts;
    }
}
