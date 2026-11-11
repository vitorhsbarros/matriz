package matriz.tasks.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import matriz.projects.ProjectMembers;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code listTaskComments}, {@code addTaskComment}, {@code editTaskComment} and {@code deleteTaskComment} in
 * openapi.yaml (US21, US22; docs/database.md: COMMENT, stored as {@code task_comment}).
 *
 * <p>Any project member reads and adds comments; only the author edits or deletes one, not even the project
 * manager (SEC11). The author is always the logged-in user. A comment stays, with its author's name, when the
 * author leaves the project (D13).
 */
@RestController
@RequestMapping("/api/projects/{projectId}/tasks/{taskId}/comments")
class CommentsController {

    static final String TEXT_REQUIRED = "Comment is required.";
    static final String TEXT_TOO_LONG = "Comment must be at most 2000 characters.";
    static final String NOT_AUTHOR = "Only the author can change this comment.";
    static final String COMMENT_NOT_FOUND = "Comment not found.";

    record CommentRequest(String text) {
    }

    record Author(UUID userId, String name) {
    }

    record CommentView(UUID id, Author author, String text, OffsetDateTime createdAt, OffsetDateTime editedAt) {
    }

    private record Row(UUID id, UUID authorId, String text, OffsetDateTime createdAt, OffsetDateTime editedAt) {
    }

    private static final String COLUMNS = "id, author_id, text, created_at, edited_at";

    private final TaskPaths paths;
    private final JdbcClient jdbc;
    private final ProjectMembers members;

    CommentsController(TaskPaths paths, JdbcClient jdbc, ProjectMembers members) {
        this.paths = paths;
        this.jdbc = jdbc;
        this.members = members;
    }

    /** US21 AC02, AC04, AC06, AC07: oldest first. */
    @GetMapping
    List<CommentView> list(@PathVariable String projectId, @PathVariable String taskId, Authentication authentication) {
        UUID task = paths.task(paths.memberProject(projectId, authentication), taskId).id();
        List<Row> rows = jdbc.sql("select " + COLUMNS + " from task_comment where task_id = ? order by created_at, id")
                .param(task).query((row, number) -> row(row)).list();
        Map<UUID, String> names = members.userNames(rows.stream().map(Row::authorId).distinct().toList());
        return rows.stream().map(row -> view(row, names)).toList();
    }

    /** US21 AC01, AC03–AC05. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CommentView add(@PathVariable String projectId, @PathVariable String taskId, @RequestBody CommentRequest body,
            Authentication authentication) {
        UUID task = paths.task(paths.memberProject(projectId, authentication), taskId).id();
        String text = text(body.text());
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into task_comment (id, task_id, author_id, text) values (?, ?, ?, ?)")
                .params(id, task, TaskPaths.user(authentication), text).update();
        return find(task, id).map(this::view).orElseThrow();
    }

    /** US22 AC01, AC05–AC08: an edit sets {@code editedAt}, so the interface shows "(edited)". */
    @PatchMapping("/{commentId}")
    CommentView edit(@PathVariable String projectId, @PathVariable String taskId, @PathVariable String commentId,
            @RequestBody CommentRequest body, Authentication authentication) {
        UUID task = paths.task(paths.memberProject(projectId, authentication), taskId).id();
        Row comment = ownComment(task, commentId, authentication);
        String text = text(body.text());
        if (!text.equals(comment.text())) {   // AC08: saving the same text changes nothing
            jdbc.sql("update task_comment set text = ?, edited_at = now() where id = ?").params(text, comment.id()).update();
        }
        return find(task, comment.id()).map(this::view).orElseThrow(() -> problem(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND));
    }

    /** US22 AC03, AC05, AC07. The browser application asks the author to confirm first (NFR05). */
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String projectId, @PathVariable String taskId, @PathVariable String commentId,
            Authentication authentication) {
        UUID task = paths.task(paths.memberProject(projectId, authentication), taskId).id();
        Row comment = ownComment(task, commentId, authentication);
        if (jdbc.sql("delete from task_comment where id = ?").param(comment.id()).update() == 0) {
            throw problem(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND);   // deleted in the meantime
        }
    }

    /** The comment, if it is in the task and the logged-in user wrote it (AC05, AC07; SEC11). */
    private Row ownComment(UUID task, String commentId, Authentication authentication) {
        UUID id;
        try {
            id = UUID.fromString(commentId);
        } catch (IllegalArgumentException notAnIdentifier) {
            throw problem(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND);   // AS19
        }
        Row comment = find(task, id).orElseThrow(() -> problem(HttpStatus.NOT_FOUND, COMMENT_NOT_FOUND));
        if (!comment.authorId().equals(TaskPaths.user(authentication))) {
            throw problem(HttpStatus.FORBIDDEN, NOT_AUTHOR);
        }
        return comment;
    }

    private Optional<Row> find(UUID task, UUID id) {
        return jdbc.sql("select " + COLUMNS + " from task_comment where id = ? and task_id = ?")
                .params(id, task).query((row, number) -> row(row)).optional();
    }

    /** Required, 1–2000 characters after trimming (US21 AC03, AC05; US22 AC06; D01, D02). */
    private static String text(String value) {
        String text = value == null ? "" : value.strip();
        if (text.isEmpty()) {
            throw problem(HttpStatus.UNPROCESSABLE_CONTENT, TEXT_REQUIRED);
        }
        if (text.codePointCount(0, text.length()) > 2000) {
            throw problem(HttpStatus.UNPROCESSABLE_CONTENT, TEXT_TOO_LONG);
        }
        return text;
    }

    private CommentView view(Row row) {
        return view(row, members.userNames(List.of(row.authorId())));
    }

    private static CommentView view(Row row, Map<UUID, String> names) {
        return new CommentView(row.id(), new Author(row.authorId(), names.get(row.authorId())), row.text(),
                row.createdAt(), row.editedAt());
    }

    private static Row row(ResultSet row) throws SQLException {
        return new Row(row.getObject("id", UUID.class), row.getObject("author_id", UUID.class), row.getString("text"),
                row.getObject("created_at", OffsetDateTime.class), row.getObject("edited_at", OffsetDateTime.class));
    }

    private static ErrorResponseException problem(HttpStatus status, String detail) {
        return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
    }
}
