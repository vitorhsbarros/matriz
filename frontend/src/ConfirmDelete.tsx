import { useState } from "react";
import { ApiError } from "./api";

type Props = {
  /** The exact confirmation text of the story, e.g. `Delete "Website" and all its tasks? This can't be undone.` */
  message: string;
  /** The confirming choice, e.g. "Delete project" or "Delete task". */
  confirmLabel: string;
  onConfirm: () => Promise<void>;
  onDeleted: () => void;
  onCancel: () => void;
};

// NFR05 (US15 AC01–AC03, US16 AC01–AC03): nothing is deleted until the user confirms.
export function ConfirmDelete({ message, confirmLabel, onConfirm, onDeleted, onCancel }: Props) {
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  async function confirm() {
    setSending(true);
    setError(null);
    try {
      await onConfirm();
      onDeleted();
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 401) {
        return;
      }
      setError(failure instanceof Error ? failure.message : String(failure));   // e.g. "Task not found."
      setSending(false);
    }
  }

  return (
    <div role="dialog" aria-label="Confirm deletion">
      <p>{message}</p>
      {error && <p role="alert">{error}</p>}
      <button type="button" onClick={confirm} disabled={sending}>{confirmLabel}</button>{" "}
      <button type="button" onClick={onCancel}>Cancel</button>
    </div>
  );
}

/** US15's confirmation text. */
export const deleteProjectMessage = (name: string) => `Delete "${name}" and all its tasks? This can't be undone.`;

/** US16's confirmation text. */
export const deleteTaskMessage = (title: string) => `Delete "${title}"? This can't be undone.`;
