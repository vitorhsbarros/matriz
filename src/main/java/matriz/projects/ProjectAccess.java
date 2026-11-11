package matriz.projects;

import java.util.UUID;

/**
 * Public interface of Projects: may this user act on this project? (04-solution-architecture.md,
 * section 3.4, <em>Is member</em>; implementation-plan.md, section 3).
 *
 * <p>Every operation on a project, or on its tasks, assignees and comments, calls one of these methods
 * with the identifier of the logged-in user, taken from the session, never from the request (NFR03, SEC02).
 */
public interface ProjectAccess {

    /**
     * Passes only if the user is a member of the project (the manager is also a member, D07).
     *
     * @throws ProjectNotFoundException if the project doesn't exist or the user isn't a member: the same
     *         answer in both cases, so users can't find out which projects exist (DoD03, SEC03)
     */
    void requireMember(UUID projectId, UUID userId);

    /**
     * Passes only if the user is the project's manager.
     *
     * @throws ProjectNotFoundException if the project doesn't exist or the user isn't a member
     * @throws ManagerOnlyException if the user is a member but not the manager
     */
    void requireManager(UUID projectId, UUID userId, ManagerOnlyAction action);
}
