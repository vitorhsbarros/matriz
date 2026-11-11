package matriz.projects.internal;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import matriz.accounts.AccountDirectory;
import matriz.accounts.AccountDirectory.Account;
import matriz.projects.MemberRemoved;
import matriz.projects.ProjectMembers;
import matriz.projects.ProjectRole;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Project memberships (docs/database.md: PROJECT_MEMBER). Names and emails come from Accounts. */
@Component
class JdbcProjectMembers implements ProjectMembers {

    private final JdbcClient jdbc;
    private final AccountDirectory accounts;
    private final ApplicationEventPublisher events;

    JdbcProjectMembers(JdbcClient jdbc, AccountDirectory accounts, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.events = events;
    }

    @Override
    public List<Member> members(UUID projectId) {
        UUID manager = managerOf(projectId);
        List<UUID> ids = jdbc.sql("select user_id from project_member where project_id = ?")
                .param(projectId).query(UUID.class).list();
        Map<UUID, Account> found = accounts.findByIds(ids);
        return ids.stream()
                .map(found::get)
                .map(account -> new Member(account.id(), account.name(), account.email(),
                        account.id().equals(manager) ? ProjectRole.MANAGER : ProjectRole.MEMBER))
                .sorted(Comparator.comparing((Member member) -> member.name().toLowerCase())
                        .thenComparing(member -> member.userId()))
                .toList();
    }

    @Override
    public boolean lockIfMember(UUID projectId, UUID userId) {
        return lockIfAllMembers(projectId, List.of(userId));
    }

    @Override
    public boolean lockIfAllMembers(UUID projectId, Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return true;
        }
        // FOR SHARE: a removal of any of these memberships waits until the caller's transaction ends (PD05).
        List<UUID> locked = jdbc.sql("""
                select user_id from project_member
                where project_id = :project and user_id in (:users)
                for share
                """)
                .param("project", projectId)
                .param("users", userIds)
                .query(UUID.class)
                .list();
        return new HashSet<>(locked).containsAll(userIds);
    }

    @Override
    public Map<UUID, String> userNames(Collection<UUID> userIds) {
        Map<UUID, String> names = new HashMap<>();
        accounts.findByIds(userIds).forEach((id, account) -> names.put(id, account.name()));
        return names;
    }

    UUID managerOf(UUID projectId) {
        return jdbc.sql("select manager_id from project where id = ?").param(projectId).query(UUID.class).single();
    }

    /** Returns false if the user is already a member (US09 AC05). */
    boolean add(UUID projectId, UUID userId) {
        return jdbc.sql("insert into project_member (project_id, user_id) values (?, ?) on conflict do nothing")
                .params(projectId, userId)
                .update() == 1;
    }

    /**
     * Removes the membership and, in the same transaction, the person from the project's tasks (D10) through
     * {@link MemberRemoved}. The row is deleted first, so it stays locked until the end (PD05). Returns false
     * if the user wasn't a member (US09 AC11).
     */
    @Transactional
    boolean remove(UUID projectId, UUID userId) {
        int deleted = jdbc.sql("delete from project_member where project_id = ? and user_id = ?")
                .params(projectId, userId)
                .update();
        if (deleted == 0) {
            return false;
        }
        events.publishEvent(new MemberRemoved(projectId, userId));
        return true;
    }
}
