import { FormEvent, useState } from "react";
import { ApiError } from "./api";

type Props = {
  submitLabel: string;
  initialName?: string;
  initialDescription?: string;
  /** Saves the fields; an error's message is shown as the API gave it (DoD04). */
  onSubmit: (name: string, description: string) => Promise<void>;
  onCancel: () => void;
};

// The project's fields (US03, US14). The limits are checked by the API, which gives the exact messages, so
// the fields aren't marked "required" and have no length limit of their own.
export function ProjectForm({ submitLabel, initialName = "", initialDescription = "", onSubmit, onCancel }: Props) {
  const [name, setName] = useState(initialName);
  const [description, setDescription] = useState(initialDescription);
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSending(true);
    setError(null);
    try {
      await onSubmit(name, description);
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 401) {
        return; // the application is already back on the log-in screen
      }
      setError(failure instanceof Error ? failure.message : String(failure));
      setSending(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate>
      {error && <p role="alert">{error}</p>}
      <p>
        <label htmlFor="project-name">Name</label>
        <br />
        <input id="project-name" placeholder="Project name" value={name}
          onChange={(event) => setName(event.target.value)} />
      </p>
      <p>
        <label htmlFor="project-description">Description (optional)</label>
        <br />
        <textarea id="project-description" value={description}
          onChange={(event) => setDescription(event.target.value)} />
      </p>
      <button type="submit" disabled={sending}>{submitLabel}</button>{" "}
      <button type="button" onClick={onCancel}>Cancel</button>
    </form>
  );
}
