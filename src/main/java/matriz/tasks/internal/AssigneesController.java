package matriz.tasks.internal;

import java.util.List;
import java.util.UUID;
import matriz.projects.ProjectMembers;
import matriz.tasks.internal.JdbcTasks.TaskView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code listEligibleAssignees}, {@code addTaskAssignee}, {@code removeTaskUserAssignee} and
 * {@code removeTaskTeamAssignee} in openapi.yaml (US10, US13). Any project member may change the assignees of
 * any task (D08); assigning or removing changes no other field of the task (US10 AC09).
 */
@RestController
class AssigneesController {

    static final String MALFORMED_BODY = "The request body is not valid.";

    /** Either {@code {type: person, userId}} or {@code {type: team, teamId}} (AddAssigneeRequest). */
    record AddAssigneeRequest(String type, String userId, String teamId) {
    }

    record EligiblePerson(UUID userId, String name) {
    }

    record EligibleTeam(UUID teamId, String name) {
    }

    record EligibleAssignees(List<EligiblePerson> people, List<EligibleTeam> teams) {
    }

    private final TaskPaths paths;
    private final TaskAssignees assignees;
    private final ProjectMembers members;

    AssigneesController(TaskPaths paths, TaskAssignees assignees, ProjectMembers members) {
        this.paths = paths;
        this.assignees = assignees;
        this.members = members;
    }

    /** People: the project's members. Teams: those whose members are all in the project now (D09). */
    @GetMapping("/api/projects/{projectId}/eligible-assignees")
    EligibleAssignees eligible(@PathVariable String projectId, Authentication authentication) {
        UUID project = paths.memberProject(projectId, authentication);
        return new EligibleAssignees(
                members.members(project).stream().map(member -> new EligiblePerson(member.userId(), member.name())).toList(),
                assignees.eligibleTeams(project).stream().map(team -> new EligibleTeam(team.id(), team.name())).toList());
    }

    @PostMapping("/api/projects/{projectId}/tasks/{taskId}/assignees")
    TaskView add(@PathVariable String projectId, @PathVariable String taskId, @RequestBody AddAssigneeRequest body,
            Authentication authentication) {
        UUID project = paths.memberProject(projectId, authentication);
        UUID task = paths.task(project, taskId).id();
        if ("person".equals(body.type()) && body.userId() != null && body.teamId() == null) {
            UUID user = identifier(body.userId());
            if (user == null) {
                // No account can have this identifier, so it isn't a member either.
                throw problem(HttpStatus.UNPROCESSABLE_CONTENT, TaskAssignees.PERSON_NOT_MEMBER);
            }
            assignees.assignPerson(project, task, user);
        } else if ("team".equals(body.type()) && body.teamId() != null && body.userId() == null) {
            UUID team = identifier(body.teamId());
            if (team == null) {
                throw problem(HttpStatus.UNPROCESSABLE_CONTENT, TaskAssignees.TEAM_NOT_FOUND);   // AS10
            }
            assignees.assignTeam(project, task, team);
        } else {
            throw problem(HttpStatus.BAD_REQUEST, MALFORMED_BODY);   // DoD07: neither shape of the schema
        }
        return assignees.attach(paths.task(project, taskId));
    }

    @DeleteMapping("/api/projects/{projectId}/tasks/{taskId}/assignees/users/{userId}")
    TaskView removePerson(@PathVariable String projectId, @PathVariable String taskId, @PathVariable String userId,
            Authentication authentication) {
        UUID project = paths.memberProject(projectId, authentication);
        UUID task = paths.task(project, taskId).id();
        UUID user = identifier(userId);
        if (user != null) {
            assignees.removePerson(task, user);
        }
        return assignees.attach(paths.task(project, taskId));
    }

    @DeleteMapping("/api/projects/{projectId}/tasks/{taskId}/assignees/teams/{teamId}")
    TaskView removeTeam(@PathVariable String projectId, @PathVariable String taskId, @PathVariable String teamId,
            Authentication authentication) {
        UUID project = paths.memberProject(projectId, authentication);
        UUID task = paths.task(project, taskId).id();
        UUID team = identifier(teamId);
        if (team != null) {
            assignees.removeTeam(task, team);
        }
        return assignees.attach(paths.task(project, taskId));
    }

    /** The identifier, or {@code null} if the text isn't one. */
    private static UUID identifier(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException notAnIdentifier) {
            return null;
        }
    }

    private static ErrorResponseException problem(HttpStatus status, String detail) {
        return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
    }
}
