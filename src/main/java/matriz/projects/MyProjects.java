package matriz.projects;

import java.util.List;
import java.util.UUID;

/**
 * Public interface of Projects: <em>Get my projects</em> (04-solution-architecture.md, section 3.4). Used by
 * the home dashboard, which the Tasks module serves because it adds the task counts.
 */
public interface MyProjects {

    record MyProject(UUID id, String name, ProjectRole role) {
    }

    /**
     * The projects the user is a member of, with their role, sorted by name ignoring case (US04 AC01, AC05).
     *
     * @param nameContains optional text: only projects whose name contains it, ignoring case and leading or
     *        trailing spaces (US23, D01). {@code null} or blank returns every project.
     */
    List<MyProject> of(UUID userId, String nameContains);
}
