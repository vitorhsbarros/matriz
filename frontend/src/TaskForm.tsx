import { FormEvent, useState } from "react";
import { ApiError, PRIORITIES, PRIORITY_LABEL, Priority, TaskInput } from "./api";

type Props = {
  submitLabel: string;
  initial?: TaskInput;
  /** Saves the fields; an error's message is shown as the API gave it (DoD04). */
  onSubmit: (input: TaskInput) => Promise<void>;
  onCancel: () => void;
};

const EMPTY: TaskInput = { title: "", description: "", dueDate: "", priority: "MEDIUM" };

// The task's fields (US05, US08; wireframe screen 2). The limits and the date rule are checked by the API,
// which gives the exact messages, so the fields have no "required", length or minimum date of their own.
export function TaskForm({ submitLabel, initial = EMPTY, onSubmit, onCancel }: Props) {
  const [input, setInput] = useState<TaskInput>(initial);
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  const set = (field: keyof TaskInput) => (event: { target: { value: string } }) =>
    setInput({ ...input, [field]: event.target.value });

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSending(true);
    setError(null);
    try {
      await onSubmit(input);
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 401) {
        return;
      }
      setError(failure instanceof Error ? failure.message : String(failure));
      setSending(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate aria-label="Task">
      {error && <p role="alert">{error}</p>}
      <p>
        <label htmlFor="task-title">Title</label>
        <br />
        <input id="task-title" value={input.title} onChange={set("title")} />
      </p>
      <p>
        <label htmlFor="task-description">Description (optional)</label>
        <br />
        <textarea id="task-description" value={input.description} onChange={set("description")} />
      </p>
      <p>
        <label htmlFor="task-due-date">Due date (optional)</label>
        <br />
        <input id="task-due-date" type="date" value={input.dueDate} onChange={set("dueDate")} />
      </p>
      <p>
        <label htmlFor="task-priority">Priority</label>
        <br />
        <select id="task-priority" value={input.priority}
          onChange={(event) => setInput({ ...input, priority: event.target.value as Priority })}>
          {PRIORITIES.map((priority) => <option key={priority} value={priority}>{PRIORITY_LABEL[priority]}</option>)}
        </select>
      </p>
      <button type="submit" disabled={sending}>{submitLabel}</button>{" "}
      <button type="button" onClick={onCancel}>Cancel</button>
    </form>
  );
}
