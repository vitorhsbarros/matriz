package matriz.projects.internal;

import static matriz.projects.internal.Problems.problem;

import java.util.List;
import java.util.UUID;
import matriz.accounts.AccountDirectory;
import matriz.projects.ManagerOnlyAction;
import matriz.projects.ProjectAccess;
import matriz.projects.ProjectMembers.Member;
import matriz.projects.ProjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code listProjectMembers}, {@code addProjectMember} and {@code removeProjectMember} in openapi.yaml (US09).
 * Any member sees the list; only the manager adds and removes members, and can't be removed (D07).
 */
@RestController
@RequestMapping("/api/projects/{projectId}/members")
class MembersController {

    static final String EMAIL_REQUIRED = "Email is required.";
    static final String CANNOT_ADD = "This user could not be added.";
    static final String ALREADY_MEMBER = "This user is already a member of the project.";
    static final String MANAGER_CANNOT_BE_REMOVED = "The project manager cannot be removed.";
    static final String MEMBER_NOT_FOUND = "Member not found.";

    record AddMemberRequest(String email) {
    }

    private final JdbcProjectMembers members;
    private final ProjectAccess access;
    private final AccountDirectory accounts;

    MembersController(JdbcProjectMembers members, ProjectAccess access, AccountDirectory accounts) {
        this.members = members;
        this.access = access;
        this.accounts = accounts;
    }

    /** US09: every member sees the names, emails and roles (PRI03). */
    @GetMapping
    List<Member> list(@PathVariable String projectId, Authentication authentication) {
        UUID project = projectId(projectId);
        access.requireMember(project, userId(authentication));
        return members.members(project);
    }

    /** US09 AC01, AC04, AC05, AC07, AC08, AC10. The answer never says whether an email has an account (SEC09). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Member add(@PathVariable String projectId, @RequestBody AddMemberRequest body, Authentication authentication) {
        UUID project = projectId(projectId);
        access.requireManager(project, userId(authentication), ManagerOnlyAction.MANAGE_MEMBERS);
        if (body.email() == null || body.email().isBlank()) {
            throw problem(HttpStatus.UNPROCESSABLE_CONTENT, EMAIL_REQUIRED);
        }
        UUID user = accounts.findUserIdByEmail(body.email())
                .orElseThrow(() -> problem(HttpStatus.UNPROCESSABLE_CONTENT, CANNOT_ADD));
        if (!members.add(project, user)) {
            throw problem(HttpStatus.CONFLICT, ALREADY_MEMBER);
        }
        return members.members(project).stream()
                .filter(member -> member.userId().equals(user))
                .findFirst()
                .orElseThrow();
    }

    /** US09 AC02, AC03 (D10), AC06, AC07, AC11. */
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable String projectId, @PathVariable String userId, Authentication authentication) {
        UUID project = projectId(projectId);
        access.requireManager(project, userId(authentication), ManagerOnlyAction.MANAGE_MEMBERS);
        UUID user = Problems.idOr(userId, () -> problem(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND));
        if (user.equals(members.managerOf(project))) {
            throw problem(HttpStatus.UNPROCESSABLE_CONTENT, MANAGER_CANNOT_BE_REMOVED);
        }
        if (!members.remove(project, user)) {
            throw problem(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
    }

    private static UUID projectId(String value) {
        return Problems.idOr(value, ProjectNotFoundException::new);   // AS19
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

}
