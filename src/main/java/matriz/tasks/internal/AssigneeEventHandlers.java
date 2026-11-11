package matriz.tasks.internal;

import matriz.projects.MemberRemoved;
import matriz.teams.TeamDeleted;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Keeps the assignees consistent with Projects and Teams. Plain, synchronous {@code @EventListener}s: they run
 * in the transaction that published the event, so a failure here rolls the whole change back
 * (04-solution-architecture.md, section 3.2, rule 4).
 */
@Component
class AssigneeEventHandlers {

    private final JdbcClient jdbc;

    AssigneeEventHandlers(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** D10, R12: the person leaves every task of that project; teams and other people stay (US09 AC03). */
    @EventListener
    void on(MemberRemoved event) {
        jdbc.sql("""
                delete from task_assignee_user
                where user_id = ? and task_id in (select id from task where project_id = ?)
                """)
                .params(event.userId(), event.projectId())
                .update();
    }

    /** FR20, R19: the team leaves every task, in any project; the other assignees stay (US18 AC02). */
    @EventListener
    void on(TeamDeleted event) {
        jdbc.sql("delete from task_assignee_team where team_id = ?").param(event.teamId()).update();
    }
}
