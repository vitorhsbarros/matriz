package matriz.projects.internal;

import java.util.List;
import java.util.UUID;
import matriz.projects.MyProjects;
import matriz.projects.ProjectDeleted;
import matriz.projects.ProjectRole;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Projects and their memberships in the database (docs/database.md: PROJECT, PROJECT_MEMBER). */
@Component
class JdbcProjects implements MyProjects {

    record ProjectView(UUID id, String name, String description, ProjectRole role) {
    }

    private final JdbcClient jdbc;
    private final ApplicationEventPublisher events;

    JdbcProjects(JdbcClient jdbc, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    /** The creator becomes the manager and a member, in one transaction (FR03, D07, R08). */
    @Transactional
    UUID create(UUID creatorId, String name, String description) {
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into project (id, name, description, manager_id) values (?, ?, ?, ?)")
                .params(id, name, description, creatorId)
                .update();
        jdbc.sql("insert into project_member (project_id, user_id) values (?, ?)")
                .params(id, creatorId)
                .update();
        return id;
    }

    /** A project the caller already checked the user may see (ProjectAccess), with the user's role. */
    ProjectView find(UUID projectId, UUID userId) {
        return jdbc.sql("""
                select id, name, description,
                       case when manager_id = :user then 'MANAGER' else 'MEMBER' end as role
                from project where id = :project
                """)
                .param("project", projectId)
                .param("user", userId)
                .query((row, number) -> new ProjectView(row.getObject("id", UUID.class), row.getString("name"),
                        row.getString("description"), ProjectRole.valueOf(row.getString("role"))))
                .single();
    }

    void update(UUID projectId, String name, String description) {
        jdbc.sql("update project set name = ?, description = ? where id = ?")
                .params(name, description, projectId)
                .update();
    }

    /**
     * Deletes the project with everything that belongs to it, in one transaction (FR05, R17): first the other
     * modules' data through {@link ProjectDeleted}, then the project, whose memberships go with it (cascade).
     */
    @Transactional
    void delete(UUID projectId) {
        events.publishEvent(new ProjectDeleted(projectId));
        jdbc.sql("delete from project where id = ?").param(projectId).update();
    }

    @Override
    public List<MyProject> of(UUID userId, String nameContains) {
        String text = nameContains == null ? "" : nameContains.strip();
        return jdbc.sql("""
                select p.id, p.name, case when p.manager_id = :user then 'MANAGER' else 'MEMBER' end as role
                from project p
                join project_member m on m.project_id = p.id and m.user_id = :user
                where :text = '' or strpos(lower(p.name), lower(:text)) > 0
                order by lower(p.name), p.id
                """)
                .param("user", userId)
                .param("text", text)
                .query((row, number) -> new MyProject(row.getObject("id", UUID.class), row.getString("name"),
                        ProjectRole.valueOf(row.getString("role"))))
                .list();
    }
}
