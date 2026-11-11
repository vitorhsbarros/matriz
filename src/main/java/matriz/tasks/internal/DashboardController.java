package matriz.tasks.internal;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import matriz.projects.MyProjects;
import matriz.projects.MyProjects.MyProject;
import matriz.projects.ProjectRole;
import matriz.tasks.internal.JdbcTaskCounts.TaskCounts;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code listMyProjects} in openapi.yaml: the home dashboard (US04, US23). Served by Tasks because it adds
 * the task counts to the user's projects, which come from Projects (implementation-plan.md, Q3).
 */
@RestController
class DashboardController {

    record ProjectSummary(UUID id, String name, ProjectRole role, TaskCounts taskCounts) {
    }

    record ProjectDashboard(TaskCounts totals, List<ProjectSummary> projects) {
    }

    private final MyProjects myProjects;
    private final JdbcTaskCounts taskCounts;

    DashboardController(MyProjects myProjects, JdbcTaskCounts taskCounts) {
        this.myProjects = myProjects;
        this.taskCounts = taskCounts;
    }

    @GetMapping("/api/projects")
    ProjectDashboard dashboard(@RequestParam(required = false) String name, Authentication authentication) {
        UUID user = UUID.fromString(authentication.getName());

        // The totals always cover every project of the user, whatever the search (D14, US23 AC05).
        List<MyProject> all = myProjects.of(user, null);
        Map<UUID, TaskCounts> counts = taskCounts.perProject(all.stream().map(MyProject::id).toList());
        TaskCounts totals = counts.values().stream().reduce(TaskCounts.NONE, TaskCounts::plus);

        List<MyProject> shown = name == null || name.isBlank() ? all : myProjects.of(user, name);
        List<ProjectSummary> projects = shown.stream()
                .map(project -> new ProjectSummary(project.id(), project.name(), project.role(),
                        counts.getOrDefault(project.id(), TaskCounts.NONE)))
                .toList();
        return new ProjectDashboard(totals, projects);
    }
}
