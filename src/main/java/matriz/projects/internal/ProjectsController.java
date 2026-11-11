package matriz.projects.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.UUID;
import matriz.projects.ManagerOnlyAction;
import matriz.projects.ProjectAccess;
import matriz.projects.ProjectNotFoundException;
import matriz.projects.internal.JdbcProjects.ProjectView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code createProject}, {@code getProject}, {@code updateProject} and {@code deleteProject} in openapi.yaml
 * (US03, US04 AC02, US14, US15). The list of the user's projects is the home dashboard, served by Tasks.
 *
 * <p>The user always comes from the session. Access is checked before anything else, so a non-member learns
 * nothing about the project, not even whether their data would be valid (DoD03).
 */
@RestController
class ProjectsController {

    record CreateProjectRequest(String name, String description) {
    }

    /** Only the fields present are changed; {@code "description": null} clears the description (US14). */
    static final class UpdateProjectRequest {
        private String name;
        private boolean hasName;
        private String description;
        private boolean hasDescription;

        @JsonProperty("name")
        void setName(String name) {
            this.name = name;
            this.hasName = true;
        }

        @JsonProperty("description")
        void setDescription(String description) {
            this.description = description;
            this.hasDescription = true;
        }
    }

    private final JdbcProjects projects;
    private final ProjectAccess access;

    ProjectsController(JdbcProjects projects, ProjectAccess access) {
        this.projects = projects;
        this.access = access;
    }

    @PostMapping("/api/projects")
    ResponseEntity<ProjectView> create(@RequestBody CreateProjectRequest body, Authentication authentication) {
        UUID user = userId(authentication);
        String name = ProjectFields.name(body.name());
        String description = ProjectFields.description(body.description());
        UUID id = projects.create(user, name, description);
        return ResponseEntity.created(URI.create("/api/projects/" + id)).body(projects.find(id, user));
    }

    @GetMapping("/api/projects/{projectId}")
    ProjectView get(@PathVariable String projectId, Authentication authentication) {
        UUID project = projectId(projectId);
        UUID user = userId(authentication);
        access.requireMember(project, user);
        return projects.find(project, user);
    }

    @PatchMapping("/api/projects/{projectId}")
    ProjectView update(@PathVariable String projectId, @RequestBody UpdateProjectRequest body,
            Authentication authentication) {
        UUID project = projectId(projectId);
        UUID user = userId(authentication);
        access.requireManager(project, user, ManagerOnlyAction.EDIT_PROJECT);

        ProjectView current = projects.find(project, user);
        // Checked in the order of the schema: name, then description (AS08). An empty body changes nothing
        // (US14 AC07).
        String name = body.hasName ? ProjectFields.name(body.name) : current.name();
        String description = body.hasDescription ? ProjectFields.description(body.description)
                : current.description();
        projects.update(project, name, description);
        return projects.find(project, user);
    }

    @DeleteMapping("/api/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String projectId, Authentication authentication) {
        UUID project = projectId(projectId);
        access.requireManager(project, userId(authentication), ManagerOnlyAction.DELETE_PROJECT);
        projects.delete(project);
    }

    /** An identifier that isn't a UUID can't name any project: "Project not found." like any other. */
    private static UUID projectId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException notAnIdentifier) {
            throw new ProjectNotFoundException();
        }
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
