import { ErrorMessage, useAction } from "./ui";

type Props = {
  /** The exact confirmation text of the story, e.g. `Delete "Website" and all its tasks? This can't be undone.` */
  message: string;
  /** The confirming choice, e.g. "Delete project", "Delete task", "Delete team" or "Delete comment". */
  confirmLabel: string;
  onConfirm: () => Promise<void>;
  onDeleted: () => void;
  onCancel: () => void;
};

// NFR05 (US15, US16, US18, US22): nothing is deleted until the user confirms. The confirming button can't send
// the request twice, and a refusal ("Task not found.", "Only the author can change this comment.") is shown here.
export function ConfirmDelete({ message, confirmLabel, onConfirm, onDeleted, onCancel }: Props) {
  const action = useAction();

  async function confirm() {
    if (await action.run(onConfirm)) {
      onDeleted();
    }
  }

  return (
    <div className="confirm" role="dialog" aria-label="Confirm deletion">
      <p>{message}</p>
      <ErrorMessage message={action.error} />
      <div className="actions">
        <button type="button" className="btn btn-primary" onClick={confirm} disabled={action.busy}>
          {confirmLabel}
        </button>
        <button type="button" className="btn" onClick={onCancel}>Cancel</button>
      </div>
    </div>
  );
}

/** US15's confirmation text. */
export const deleteProjectMessage = (name: string) => `Delete "${name}" and all its tasks? This can't be undone.`;

/** US16's confirmation text. */
export const deleteTaskMessage = (title: string) => `Delete "${title}"? This can't be undone.`;

/** US18's confirmation text. */
export const deleteTeamMessage = (name: string) =>
  `Delete the team "${name}"? It will be removed from its tasks. This can't be undone.`;

/** US22's confirmation text. */
export const DELETE_COMMENT_MESSAGE = "Delete this comment? This can't be undone.";
