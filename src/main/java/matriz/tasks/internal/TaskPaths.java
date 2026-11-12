package matriz.tasks.internal;

import java.util.UUID;
import matriz.projects.ProjectAccess;
import matriz.projects.ProjectNotFoundException;
import matriz.tasks.internal.JdbcTasks.TaskView;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * The project and the task named in a URL under {@code /api/projects/{projectId}/tasks/{taskId}}, checked in
 * this order: the user is a member of the project (DoD03), then the task is in that project (DoD05).
 */
@Component
class TaskPaths {

    private final ProjectAccess access;
    private final JdbcTasks tasks;

    TaskPaths(ProjectAccess access, JdbcTasks tasks) {
        this.access = access;
        this.tasks = tasks;
    }

    UUID memberProject(String projectId, Authentication authentication) {
        UUID project = Problems.idOr(projectId, ProjectNotFoundException::new);   // AS19
        access.requireMember(project, user(authentication));
        return project;
    }

    TaskView task(UUID projectId, String taskId) {
        return tasks.find(projectId, TasksController.taskId(taskId)).orElseThrow(TasksController::taskNotFound);
    }

    static UUID user(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
