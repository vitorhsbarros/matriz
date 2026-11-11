package matriz.teams;

import java.util.UUID;

/**
 * Event <em>team deleted</em> (FR20, R19). Published inside the transaction that deletes the team,
 * <strong>before</strong> the team's row is deleted, and handled synchronously in the same transaction: the
 * Tasks module removes the team from every task's assignees first, because that foreign key refuses the delete
 * otherwise (PD04). If a handler fails, the whole deletion is rolled back.
 */
public record TeamDeleted(UUID teamId) {
}
