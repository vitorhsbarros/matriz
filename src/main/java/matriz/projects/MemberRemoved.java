package matriz.projects;

import java.util.UUID;

/**
 * Event <em>member removed</em> (D10, R12; 04-solution-architecture.md, section 4.2).
 *
 * <p>Published inside the transaction that removes the membership, <strong>after</strong> the membership's row
 * is deleted, and handled synchronously in the same transaction: the Tasks module removes the person from the
 * assignees of the project's tasks. Deleting the row first keeps it locked until the end of the transaction,
 * so an assignment made at the same time either waits and fails, or is removed here (PD05).
 */
public record MemberRemoved(UUID projectId, UUID userId) {
}
