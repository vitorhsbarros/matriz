package matriz.projects;

import java.util.UUID;

/**
 * Event <em>project deleted</em> (FR05, R17; 04-solution-architecture.md, section 3.2, rule 4).
 *
 * <p>Published inside the transaction that deletes the project, <strong>before</strong> the project's row is
 * deleted, and handled synchronously in the same transaction: the Tasks module deletes the project's tasks
 * first, because their foreign key to the project refuses the delete otherwise (PD04). If a handler fails,
 * the whole deletion is rolled back, so no orphan rows remain.
 */
public record ProjectDeleted(UUID projectId) {
}
