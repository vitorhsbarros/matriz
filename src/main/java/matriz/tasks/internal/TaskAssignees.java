package matriz.tasks.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import matriz.projects.ProjectMembers;
import matriz.tasks.internal.JdbcTasks.AssigneeView;
import matriz.tasks.internal.JdbcTasks.TaskView;
import matriz.teams.TeamDirectory;
import matriz.teams.TeamDirectory.TeamInfo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

/**
 * A task's assignees: people and teams, each at most once (FR14, R04; docs/database.md: TASK_ASSIGNEE_USER,
 * TASK_ASSIGNEE_TEAM). Names come from Projects (people, also after they left, D13) and Teams.
 */
@Component
class TaskAssignees {

    static final String PERSON_NOT_MEMBER = "The assignee must be a member of the project.";
    static final String TEAM_NOT_IN_PROJECT = "All team members must belong to the project.";
    static final String TEAM_NOT_FOUND = "Team not found.";

    private final JdbcClient jdbc;
    private final ProjectMembers members;
    private final TeamDirectory teams;

    TaskAssignees(JdbcClient jdbc, ProjectMembers members, TeamDirectory teams) {
        this.jdbc = jdbc;
        this.members = members;
        this.teams = teams;
    }

    /**
     * The tasks with their assignees, sorted by the name shown ignoring case ("Ana Silva, Design (team)",
     * US06 AC01, US10 AC02, US13 AC02; api-contract.md, AS21).
     */
    List<TaskView> attach(List<TaskView> tasks) {
        if (tasks.isEmpty()) {
            return tasks;
        }
        List<UUID> taskIds = tasks.stream().map(TaskView::id).toList();
        Map<UUID, List<UUID>> people = ids("select task_id, user_id as id from task_assignee_user where task_id in (:tasks)", taskIds);
        Map<UUID, List<UUID>> teamIds = ids("select task_id, team_id as id from task_assignee_team where task_id in (:tasks)", taskIds);
        Map<UUID, String> personNames = members.userNames(people.values().stream().flatMap(List::stream).toList());
        Map<UUID, String> teamNames = teams.names(teamIds.values().stream().flatMap(List::stream).toList());

        return tasks.stream().map(task -> {
            List<AssigneeView> assignees = new ArrayList<>();
            people.getOrDefault(task.id(), List.of()).forEach(id ->
                    assignees.add(new AssigneeView("person", id, null, personNames.get(id))));
            teamIds.getOrDefault(task.id(), List.of()).forEach(id ->
                    assignees.add(new AssigneeView("team", null, id, teamNames.get(id))));
            assignees.sort(Comparator.comparing((AssigneeView assignee) -> shown(assignee).toLowerCase())
                    .thenComparing(assignee -> String.valueOf(assignee.userId() != null ? assignee.userId() : assignee.teamId())));
            return task.withAssignees(assignees);
        }).toList();
    }

    TaskView attach(TaskView task) {
        return attach(List.of(task)).get(0);
    }

    /**
     * US10 AC01, AC02, AC04, AC05, AC07. The membership is checked and locked in this transaction, so a removal
     * of that member at the same time waits, and then removes this assignment too (PD05, D10).
     */
    @Transactional
    void assignPerson(UUID projectId, UUID taskId, UUID userId) {
        lockTask(projectId, taskId);
        if (!members.lockIfMember(projectId, userId)) {
            throw problem(HttpStatus.UNPROCESSABLE_CONTENT, PERSON_NOT_MEMBER);
        }
        jdbc.sql("insert into task_assignee_user (task_id, user_id) values (?, ?) on conflict do nothing")
                .params(taskId, userId).update();
    }

    /** US13 AC01, AC02, AC04–AC06, AC09: every member of the team must be in the project now (D09, PD05). */
    @Transactional
    void assignTeam(UUID projectId, UUID taskId, UUID teamId) {
        lockTask(projectId, taskId);
        TeamInfo team = teams.find(teamId).orElseThrow(() -> problem(HttpStatus.UNPROCESSABLE_CONTENT, TEAM_NOT_FOUND));
        if (!members.lockIfAllMembers(projectId, team.memberIds())) {
            throw problem(HttpStatus.UNPROCESSABLE_CONTENT, TEAM_NOT_IN_PROJECT);
        }
        try {
            jdbc.sql("insert into task_assignee_team (task_id, team_id) values (?, ?) on conflict do nothing")
                    .params(taskId, teamId).update();
        } catch (DataIntegrityViolationException teamDeletedInTheMeantime) {
            throw problem(HttpStatus.UNPROCESSABLE_CONTENT, TEAM_NOT_FOUND);   // AS10
        }
    }

    /** US10 AC03, AC08, AC10: removing someone who isn't an assignee changes nothing. */
    void removePerson(UUID taskId, UUID userId) {
        jdbc.sql("delete from task_assignee_user where task_id = ? and user_id = ?").params(taskId, userId).update();
    }

    /** US13 AC03. */
    void removeTeam(UUID taskId, UUID teamId) {
        jdbc.sql("delete from task_assignee_team where task_id = ? and team_id = ?").params(taskId, teamId).update();
    }

    /** Teams whose members are all project members right now, for the assignee selector (US13, D09). */
    List<TeamInfo> eligibleTeams(UUID projectId) {
        Set<UUID> projectMembers = new HashSet<>(members.members(projectId).stream()
                .map(ProjectMembers.Member::userId).toList());
        return teams.all().stream().filter(team -> projectMembers.containsAll(team.memberIds())).toList();
    }

    /** Keeps the task from being deleted until the assignment is saved; "Task not found." if it is gone. */
    private void lockTask(UUID projectId, UUID taskId) {
        boolean exists = jdbc.sql("select id from task where id = ? and project_id = ? for share")
                .params(taskId, projectId).query(UUID.class).optional().isPresent();
        if (!exists) {
            throw problem(HttpStatus.NOT_FOUND, TasksController.TASK_NOT_FOUND);
        }
    }

    private Map<UUID, List<UUID>> ids(String sql, List<UUID> taskIds) {
        Map<UUID, List<UUID>> byTask = new HashMap<>();
        jdbc.sql(sql).param("tasks", taskIds)
                .query((row, number) -> byTask.computeIfAbsent(row.getObject("task_id", UUID.class), id -> new ArrayList<>())
                        .add(row.getObject("id", UUID.class)))
                .list();
        return byTask;
    }

    private static String shown(AssigneeView assignee) {
        String name = assignee.name() == null ? "" : assignee.name();
        return "team".equals(assignee.type()) ? name + " (team)" : name;
    }

    private static ErrorResponseException problem(HttpStatus status, String detail) {
        return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
    }
}
