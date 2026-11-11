package matriz.tasks.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import matriz.projects.ProjectAccess;
import matriz.projects.ProjectNotFoundException;
import matriz.tasks.internal.JdbcTasks.Fields;
import matriz.tasks.internal.JdbcTasks.TaskView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code listProjectTasks}, {@code createTask}, {@code getTask}, {@code updateTask} and {@code deleteTask} in
 * openapi.yaml (US05, US06, US07, US08, US20, US16, US24).
 *
 * <p>Every operation first checks that the logged-in user is a member of the project (DoD03): any member may
 * change, move and delete any task (D08). A task that isn't in that project gets "Task not found." (DoD05).
 */
@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
class TasksController {

    static final String TASK_NOT_FOUND = "Task not found.";

    record CreateTaskRequest(String title, String description, String dueDate, String priority) {
    }

    /** Only the fields present are changed; {@code null} clears the description or the due date (US08). */
    static final class UpdateTaskRequest {
        private String title;
        private boolean hasTitle;
        private String description;
        private boolean hasDescription;
        private String dueDate;
        private boolean hasDueDate;
        private String status;
        private boolean hasStatus;
        private String priority;
        private boolean hasPriority;

        @JsonProperty("title")
        void setTitle(String title) {
            this.title = title;
            this.hasTitle = true;
        }

        @JsonProperty("description")
        void setDescription(String description) {
            this.description = description;
            this.hasDescription = true;
        }

        @JsonProperty("dueDate")
        void setDueDate(String dueDate) {
            this.dueDate = dueDate;
            this.hasDueDate = true;
        }

        @JsonProperty("status")
        void setStatus(String status) {
            this.status = status;
            this.hasStatus = true;
        }

        @JsonProperty("priority")
        void setPriority(String priority) {
            this.priority = priority;
            this.hasPriority = true;
        }
    }

    private final JdbcTasks tasks;
    private final TaskAssignees assignees;
    private final ProjectAccess access;
    private final Clock clock;

    TasksController(JdbcTasks tasks, TaskAssignees assignees, ProjectAccess access, Clock clock) {
        this.tasks = tasks;
        this.assignees = assignees;
        this.access = access;
        this.clock = clock;
    }

    /** US06, US24: newest first; {@code status} and {@code priority} may be repeated. */
    @GetMapping
    List<TaskView> list(@PathVariable String projectId, @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) List<String> priority, Authentication authentication) {
        UUID project = memberProject(projectId, authentication);
        List<String> statuses = status == null ? List.of() : status.stream().map(TaskFields::status).toList();
        List<String> priorities = priority == null ? List.of()
                : priority.stream().map(TaskFields::priority).toList();
        return assignees.attach(tasks.list(project, statuses, priorities));
    }

    /** US05: fields checked in the order title, description, due date, priority (AS08). */
    @PostMapping
    ResponseEntity<TaskView> create(@PathVariable String projectId, @RequestBody CreateTaskRequest body,
            Authentication authentication) {
        UUID project = memberProject(projectId, authentication);
        String title = TaskFields.title(body.title());
        String description = TaskFields.description(body.description());
        LocalDate dueDate = TaskFields.dueDate(body.dueDate(), today(), null);
        String priority = body.priority() == null ? "MEDIUM" : TaskFields.priority(body.priority());

        UUID id = tasks.create(project, title, description, dueDate, priority);
        return ResponseEntity.created(URI.create("/api/projects/" + project + "/tasks/" + id))
                .body(assignees.attach(tasks.find(project, id).orElseThrow()));
    }

    /** US20. */
    @GetMapping("/{taskId}")
    TaskView get(@PathVariable String projectId, @PathVariable String taskId, Authentication authentication) {
        UUID project = memberProject(projectId, authentication);
        return assignees.attach(tasks.find(project, taskId(taskId)).orElseThrow(TasksController::taskNotFound));
    }

    /**
     * US07 (status) and US08 (fields, priority). All or nothing: every field is checked before anything is
     * saved (US08 AC05). An empty body changes nothing (US08 AC08, AS20).
     */
    @PatchMapping("/{taskId}")
    TaskView update(@PathVariable String projectId, @PathVariable String taskId,
            @RequestBody UpdateTaskRequest body, Authentication authentication) {
        UUID project = memberProject(projectId, authentication);
        UUID task = taskId(taskId);
        TaskView current = tasks.find(project, task).orElseThrow(TasksController::taskNotFound);

        // Checked in the order of the schema: title, description, due date, status, priority (AS08).
        Fields fields = new Fields(
                body.hasTitle ? TaskFields.title(body.title) : current.title(),
                body.hasDescription ? TaskFields.description(body.description) : current.description(),
                body.hasDueDate ? TaskFields.dueDate(body.dueDate, today(), current.dueDate()) : current.dueDate(),
                body.hasStatus ? TaskFields.status(body.status) : current.status(),
                body.hasPriority ? TaskFields.priority(body.priority) : current.priority());
        if (!tasks.update(project, task, fields)) {
            throw taskNotFound();   // deleted by another member in the meantime
        }
        return assignees.attach(tasks.find(project, task).orElseThrow(TasksController::taskNotFound));
    }

    /** US16: any member may delete any task (D08). */
    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String projectId, @PathVariable String taskId, Authentication authentication) {
        UUID project = memberProject(projectId, authentication);
        if (!tasks.delete(project, taskId(taskId))) {
            throw taskNotFound();   // US16 AC05: already deleted by another member
        }
    }

    /** The project in the URL, once the logged-in user is known to be one of its members (DoD03). */
    UUID memberProject(String projectId, Authentication authentication) {
        UUID project;
        try {
            project = UUID.fromString(projectId);
        } catch (IllegalArgumentException notAnIdentifier) {
            throw new ProjectNotFoundException();   // AS19
        }
        access.requireMember(project, UUID.fromString(authentication.getName()));
        return project;
    }

    static UUID taskId(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException notAnIdentifier) {
            throw taskNotFound();   // AS19
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);   // AS11: the server's current date
    }

    static ErrorResponseException taskNotFound() {
        return new ErrorResponseException(HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, TASK_NOT_FOUND), null);
    }
}
