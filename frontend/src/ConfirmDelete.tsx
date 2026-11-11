import { useState } from "react";
import { ApiError, deleteProject } from "./api";

type Props = {
  project: { id: string; name: string };
  onDeleted: () => void;
  onCancel: () => void;
};

// US15 AC01–AC03 (NFR05): nothing is deleted until the user confirms, with the exact text of the story.
export function ConfirmDelete({ project, onDeleted, onCancel }: Props) {
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  async function confirm() {
    setSending(true);
    setError(null);
    try {
      await deleteProject(project.id);
      onDeleted();
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 401) {
        return;
      }
      setError(failure instanceof Error ? failure.message : String(failure));
      setSending(false);
    }
  }

  return (
    <div role="dialog" aria-label="Confirm deletion">
      <p>{`Delete "${project.name}" and all its tasks? This can't be undone.`}</p>
      {error && <p role="alert">{error}</p>}
      <button type="button" onClick={confirm} disabled={sending}>Delete project</button>{" "}
      <button type="button" onClick={onCancel}>Cancel</button>
    </div>
  );
}
