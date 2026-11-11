package matriz.teams.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import matriz.teams.TeamDeleted;
import matriz.teams.TeamDirectory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Teams and their memberships (docs/database.md: TEAM, TEAM_MEMBER). */
@Component
class JdbcTeams implements TeamDirectory {

    record TeamRow(UUID id, String name, UUID managerId) {
    }

    private final JdbcClient jdbc;
    private final ApplicationEventPublisher events;

    JdbcTeams(JdbcClient jdbc, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    Optional<TeamRow> row(UUID teamId) {
        return jdbc.sql("select id, name, manager_id from team where id = ?")
                .param(teamId)
                .query((row, number) -> new TeamRow(row.getObject("id", UUID.class), row.getString("name"),
                        row.getObject("manager_id", UUID.class)))
                .optional();
    }

    List<TeamRow> rows() {
        return jdbc.sql("select id, name, manager_id from team order by lower(name), id")
                .query((row, number) -> new TeamRow(row.getObject("id", UUID.class), row.getString("name"),
                        row.getObject("manager_id", UUID.class)))
                .list();
    }

    /** Every team's members, by team. */
    Map<UUID, List<UUID>> memberIds(Collection<UUID> teamIds) {
        Map<UUID, List<UUID>> members = new HashMap<>();
        teamIds.forEach(id -> members.put(id, new ArrayList<>()));
        if (teamIds.isEmpty()) {
            return members;
        }
        jdbc.sql("select team_id, user_id from team_member where team_id in (:teams)")
                .param("teams", teamIds)
                .query((row, number) -> {
                    members.get(row.getObject("team_id", UUID.class)).add(row.getObject("user_id", UUID.class));
                    return null;
                })
                .list();
        return members;
    }

    /** Another team already has this name, ignoring case (D06). {@code except} is the team being renamed. */
    boolean nameTaken(String name, UUID except) {
        return jdbc.sql("select count(*) from team where lower(name) = lower(?) and id <> ?")
                .params(name, except == null ? new UUID(0, 0) : except)
                .query(Integer.class)
                .single() > 0;
    }

    /** The creator becomes the manager and a member, in one transaction (FR08, D07). */
    @Transactional
    UUID create(UUID creatorId, String name) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into team (id, name, manager_id) values (?, ?, ?)").params(id, name, creatorId).update();
        jdbc.sql("insert into team_member (team_id, user_id) values (?, ?)").params(id, creatorId).update();
        return id;
    }

    void rename(UUID teamId, String name) {
        jdbc.sql("update team set name = ? where id = ?").params(name, teamId).update();
    }

    /**
     * Deletes the team, in one transaction (FR20): first its place among the tasks' assignees through
     * {@link TeamDeleted}, then the team, whose memberships go with it (cascade).
     */
    @Transactional
    void delete(UUID teamId) {
        events.publishEvent(new TeamDeleted(teamId));
        jdbc.sql("delete from team where id = ?").param(teamId).update();
    }

    /** Returns false if the user is already a member (US12 AC04). */
    boolean addMember(UUID teamId, UUID userId) {
        return jdbc.sql("insert into team_member (team_id, user_id) values (?, ?) on conflict do nothing")
                .params(teamId, userId)
                .update() == 1;
    }

    /** Existing task assignments to the team stay (D09). Returns false if the user wasn't a member. */
    boolean removeMember(UUID teamId, UUID userId) {
        return jdbc.sql("delete from team_member where team_id = ? and user_id = ?")
                .params(teamId, userId)
                .update() == 1;
    }

    @Override
    public Optional<TeamInfo> find(UUID teamId) {
        return row(teamId).map(team -> new TeamInfo(team.id(), team.name(), memberIds(List.of(team.id())).get(team.id())));
    }

    @Override
    public List<TeamInfo> all() {
        List<TeamRow> teams = rows();
        Map<UUID, List<UUID>> members = memberIds(teams.stream().map(TeamRow::id).toList());
        return teams.stream().map(team -> new TeamInfo(team.id(), team.name(), members.get(team.id()))).toList();
    }

    @Override
    public Map<UUID, String> names(Collection<UUID> teamIds) {
        Map<UUID, String> names = new HashMap<>();
        if (teamIds.isEmpty()) {
            return names;
        }
        jdbc.sql("select id, name from team where id in (:teams)")
                .param("teams", teamIds)
                .query((row, number) -> names.put(row.getObject("id", UUID.class), row.getString("name")))
                .list();
        return names;
    }
}
