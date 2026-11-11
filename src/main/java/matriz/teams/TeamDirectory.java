package matriz.teams;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Public interface of Teams for the Tasks module (04-solution-architecture.md, section 3.4): a team with its
 * members, used when assigning a team (US13, D09), and the teams' names, shown with their tasks (US06, US17).
 */
public interface TeamDirectory {

    record TeamInfo(UUID id, String name, List<UUID> memberIds) {
    }

    Optional<TeamInfo> find(UUID teamId);

    /** Every team, sorted by name ignoring case. Every user may see every team (D05, PD11). */
    List<TeamInfo> all();

    Map<UUID, String> names(Collection<UUID> teamIds);
}
