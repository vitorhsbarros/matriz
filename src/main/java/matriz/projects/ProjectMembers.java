package matriz.projects;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public interface of Projects for the modules that depend on it (04-solution-architecture.md, section 3.4):
 * <em>Get members</em>, <em>Is member</em> and <em>Are all members</em> as used when assigning, and
 * <em>Get user names</em>.
 */
public interface ProjectMembers {

    record Member(UUID userId, String name, String email, ProjectRole role) {
    }

    /** The project's members, sorted by name ignoring case (US09, AS13). */
    List<Member> members(UUID projectId);

    /**
     * True if the user is a member of the project, and keeps that membership from being removed until the
     * caller's transaction ends (PD05: the row is read {@code FOR SHARE}). Must run inside a transaction.
     */
    boolean lockIfMember(UUID projectId, UUID userId);

    /** Like {@link #lockIfMember}, for every given user at once (US13 AC04, D09). */
    boolean lockIfAllMembers(UUID projectId, Collection<UUID> userIds);

    /** The names of these users, whether or not they are still members of any project (D13). */
    Map<UUID, String> userNames(Collection<UUID> userIds);
}
