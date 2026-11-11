package matriz.projects.internal;

import java.util.Optional;
import java.util.UUID;
import matriz.projects.ManagerOnlyAction;
import matriz.projects.ManagerOnlyException;
import matriz.projects.ProjectAccess;
import matriz.projects.ProjectNotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class JdbcProjectAccess implements ProjectAccess {

    private record Role(boolean member, boolean manager) {
    }

    private final JdbcClient jdbc;

    JdbcProjectAccess(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void requireMember(UUID projectId, UUID userId) {
        roleOf(projectId, userId).filter(Role::member).orElseThrow(ProjectNotFoundException::new);
    }

    @Override
    public void requireManager(UUID projectId, UUID userId, ManagerOnlyAction action) {
        Role role = roleOf(projectId, userId).filter(Role::member).orElseThrow(ProjectNotFoundException::new);
        if (!role.manager()) {
            throw new ManagerOnlyException(action);
        }
    }

    /** Empty when the project doesn't exist. The role comes from the data, never from the request. */
    private Optional<Role> roleOf(UUID projectId, UUID userId) {
        return jdbc.sql("""
                select m.user_id is not null as member, p.manager_id = :user as manager
                from project p
                left join project_member m on m.project_id = p.id and m.user_id = :user
                where p.id = :project
                """)
                .param("project", projectId)
                .param("user", userId)
                .query((row, number) -> new Role(row.getBoolean("member"), row.getBoolean("manager")))
                .optional();
    }
}
