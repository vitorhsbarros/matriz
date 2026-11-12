package matriz.teams.internal;

import static matriz.teams.internal.Problems.problem;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import matriz.accounts.AccountDirectory;
import matriz.accounts.AccountDirectory.Account;
import matriz.teams.internal.JdbcTeams.TeamRow;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
 * {@code listTeams}, {@code createTeam}, {@code renameTeam}, {@code deleteTeam}, {@code addTeamMember} and
 * {@code removeTeamMember} in openapi.yaml (US11, US12, US17, US18).
 *
 * <p>Every logged-in user sees every team, with its members' names but not their emails (D05, PRI04, PD11).
 * Only the team's manager changes it, and the manager can't be removed (D07). Team membership gives no access
 * to any project (US12 AC08).
 */
@RestController
@RequestMapping("/api/teams")
class TeamsController {

    static final String NAME_REQUIRED = "Team name is required.";
    static final String NAME_TOO_LONG = "Team name must be at most 50 characters.";
    static final String NAME_TAKEN = "A team with this name already exists.";
    static final String TEAM_NOT_FOUND = "Team not found.";
    static final String ONLY_MANAGER_EDITS = "Only the team manager can edit the team.";
    static final String ONLY_MANAGER_DELETES = "Only the team manager can delete the team.";
    static final String ONLY_MANAGER_MANAGES = "Only the team manager can manage members.";
    static final String EMAIL_REQUIRED = "Email is required.";
    static final String CANNOT_ADD = "This user could not be added.";
    static final String ALREADY_MEMBER = "This user is already a member of the team.";
    static final String MANAGER_CANNOT_BE_REMOVED = "The team manager cannot be removed.";
    static final String MEMBER_NOT_FOUND = "Member not found.";

    record NameRequest(String name) {
    }

    record AddMemberRequest(String email) {
    }

    record TeamMemberView(UUID userId, String name, String role) {
    }

    record TeamView(UUID id, String name, List<TeamMemberView> members) {
    }

    private final JdbcTeams teams;
    private final AccountDirectory accounts;

    TeamsController(JdbcTeams teams, AccountDirectory accounts) {
        this.teams = teams;
        this.accounts = accounts;
    }

    /** Every team, sorted by name, each with its members sorted by name (US11, US12, AS13). */
    @GetMapping
    List<TeamView> list() {
        List<TeamRow> rows = teams.rows();
        Map<UUID, List<UUID>> members = teams.memberIds(rows.stream().map(TeamRow::id).toList());
        Map<UUID, Account> people = accounts.findByIds(members.values().stream().flatMap(List::stream).toList());
        return rows.stream().map(team -> view(team, members.get(team.id()), people)).toList();
    }

    /** US11 AC01–AC05. */
    @PostMapping
    ResponseEntity<TeamView> create(@RequestBody NameRequest body, Authentication authentication) {
        String name = name(body.name());
        if (teams.nameTaken(name, null)) {
            throw problem(HttpStatus.CONFLICT, NAME_TAKEN);
        }
        UUID id;
        try {
            id = teams.create(userId(authentication), name);
        } catch (DuplicateKeyException createdAtTheSameTime) {
            throw problem(HttpStatus.CONFLICT, NAME_TAKEN);
        }
        return ResponseEntity.created(URI.create("/api/teams/" + id)).body(view(id));
    }

    /** US17 AC01–AC07. Changing only the casing of the team's own name is allowed (AC06). */
    @PatchMapping("/{teamId}")
    TeamView rename(@PathVariable String teamId, @RequestBody NameRequest body, Authentication authentication) {
        TeamRow team = managedTeam(teamId, authentication, ONLY_MANAGER_EDITS);
        String name = name(body.name());
        if (teams.nameTaken(name, team.id())) {
            throw problem(HttpStatus.CONFLICT, NAME_TAKEN);
        }
        try {
            teams.rename(team.id(), name);
        } catch (DuplicateKeyException renamedAtTheSameTime) {
            throw problem(HttpStatus.CONFLICT, NAME_TAKEN);
        }
        return view(team.id());
    }

    /** US18 AC02, AC04–AC07: the team also leaves every task it was assigned to (FR20). */
    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String teamId, Authentication authentication) {
        teams.delete(managedTeam(teamId, authentication, ONLY_MANAGER_DELETES).id());
    }

    /** US12 AC01, AC03, AC04, AC06, AC07, AC09, AC11. The answer never says whether an email has an account (SEC09). */
    @PostMapping("/{teamId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    TeamMemberView addMember(@PathVariable String teamId, @RequestBody AddMemberRequest body,
            Authentication authentication) {
        TeamRow team = managedTeam(teamId, authentication, ONLY_MANAGER_MANAGES);
        if (body.email() == null || body.email().isBlank()) {
            throw problem(HttpStatus.UNPROCESSABLE_CONTENT, EMAIL_REQUIRED);
        }
        UUID user = accounts.findUserIdByEmail(body.email())
                .orElseThrow(() -> problem(HttpStatus.UNPROCESSABLE_CONTENT, CANNOT_ADD));
        if (!teams.addMember(team.id(), user)) {
            throw problem(HttpStatus.CONFLICT, ALREADY_MEMBER);
        }
        return new TeamMemberView(user, accounts.findByIds(List.of(user)).get(user).name(), "MEMBER");
    }

    /** US12 AC02, AC05, AC06, AC10, AC11. */
    @DeleteMapping("/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeMember(@PathVariable String teamId, @PathVariable String userId, Authentication authentication) {
        TeamRow team = managedTeam(teamId, authentication, ONLY_MANAGER_MANAGES);
        UUID user = Problems.idOr(userId, () -> problem(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND));
        if (user.equals(team.managerId())) {
            throw problem(HttpStatus.UNPROCESSABLE_CONTENT, MANAGER_CANNOT_BE_REMOVED);
        }
        if (!teams.removeMember(team.id(), user)) {
            throw problem(HttpStatus.NOT_FOUND, MEMBER_NOT_FOUND);
        }
    }

    /** The team, once the logged-in user is known to be its manager; "Team not found." first (US12 AC11). */
    private TeamRow managedTeam(String teamId, Authentication authentication, String refusal) {
        TeamRow team = teams.row(Problems.idOr(teamId, () -> problem(HttpStatus.NOT_FOUND, TEAM_NOT_FOUND)))
                .orElseThrow(() -> problem(HttpStatus.NOT_FOUND, TEAM_NOT_FOUND));
        if (!team.managerId().equals(userId(authentication))) {
            throw problem(HttpStatus.FORBIDDEN, refusal);
        }
        return team;
    }

    /** Required, 1–50 characters after trimming (US11 AC02, AC05; US17 AC03, AC07; D01, D02). */
    private static String name(String value) {
        String name = value == null ? "" : value.strip();
        if (name.isEmpty()) {
            throw problem(HttpStatus.UNPROCESSABLE_CONTENT, NAME_REQUIRED);
        }
        if (name.codePointCount(0, name.length()) > 50) {
            throw problem(HttpStatus.UNPROCESSABLE_CONTENT, NAME_TOO_LONG);
        }
        return name;
    }

    private TeamView view(UUID teamId) {
        TeamRow team = teams.row(teamId).orElseThrow();
        List<UUID> members = teams.memberIds(List.of(teamId)).get(teamId);
        return view(team, members, accounts.findByIds(members));
    }

    private static TeamView view(TeamRow team, List<UUID> memberIds, Map<UUID, Account> people) {
        List<TeamMemberView> members = memberIds.stream()
                .map(id -> new TeamMemberView(id, people.get(id).name(),
                        id.equals(team.managerId()) ? "MANAGER" : "MEMBER"))
                .sorted(Comparator.comparing((TeamMemberView member) -> member.name().toLowerCase())
                        .thenComparing(TeamMemberView::userId))
                .toList();
        return new TeamView(team.id(), team.name(), members);
    }

    private static UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

}
