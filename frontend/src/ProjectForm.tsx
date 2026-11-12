import { FormEvent, useState } from "react";
import { ErrorMessage, Field, useAction } from "./ui";

type Props = {
  title: string;
  submitLabel: string;
  initialName?: string;
  initialDescription?: string;
  /** Saves the fields; an error's message is shown as the API gave it (DoD04). */
  onSubmit: (name: string, description: string) => Promise<void>;
  onCancel: () => void;
};

// The project's fields (US03, US14). The limits are checked by the API, which gives the exact messages, so the
// fields aren't marked "required" and have no length limit of their own. On a failure the typed text stays.
export function ProjectForm({ title, submitLabel, initialName = "", initialDescription = "", onSubmit, onCancel }: Props) {
  const [name, setName] = useState(initialName);
  const [description, setDescription] = useState(initialDescription);
  const action = useAction();

  function submit(event: FormEvent) {
    event.preventDefault();
    action.run(() => onSubmit(name, description));
  }

  return (
    <form className="panel form" onSubmit={submit} noValidate aria-label={title}>
      <h2>{title}</h2>
      <ErrorMessage message={action.error} />
      <Field id="project-name" label="Name">
        <input id="project-name" className="input" placeholder="Project name" value={name}
          onChange={(event) => setName(event.target.value)} />
      </Field>
      <Field id="project-description" label="Description (optional)">
        <textarea id="project-description" className="input" value={description}
          onChange={(event) => setDescription(event.target.value)} />
      </Field>
      <div className="actions">
        <button type="submit" className="btn btn-primary" disabled={action.busy}>{submitLabel}</button>
        <button type="button" className="btn" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}
