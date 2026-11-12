import { FormEvent, useState } from "react";
import { PRIORITIES, PRIORITY_LABEL, Priority, TaskInput } from "./api";
import { ErrorMessage, Field, useAction } from "./ui";

type Props = {
  title: string;
  submitLabel: string;
  initial?: TaskInput;
  /** Saves the fields; an error's message is shown as the API gave it (DoD04). */
  onSubmit: (input: TaskInput) => Promise<void>;
  onCancel: () => void;
};

const EMPTY: TaskInput = { title: "", description: "", dueDate: "", priority: "MEDIUM" };

// The task's fields (US05, US08; wireframe screen 2). The limits and the date rule are checked by the API, which
// gives the exact messages, so the fields have no "required", length or minimum date of their own. On a failure
// the typed values stay in the form.
export function TaskForm({ title, submitLabel, initial = EMPTY, onSubmit, onCancel }: Props) {
  const [input, setInput] = useState<TaskInput>(initial);
  const action = useAction();

  const set = (field: keyof TaskInput) => (event: { target: { value: string } }) =>
    setInput({ ...input, [field]: event.target.value });

  function submit(event: FormEvent) {
    event.preventDefault();
    action.run(() => onSubmit(input));
  }

  return (
    <form className="panel form" onSubmit={submit} noValidate aria-label={title}>
      <h2>{title}</h2>
      <ErrorMessage message={action.error} />
      <Field id="task-title" label="Title">
        <input id="task-title" className="input" value={input.title} onChange={set("title")} />
      </Field>
      <Field id="task-description" label="Description (optional)">
        <textarea id="task-description" className="input" value={input.description} onChange={set("description")} />
      </Field>
      <Field id="task-due-date" label="Due date (optional)">
        <input id="task-due-date" className="input" type="date" value={input.dueDate} onChange={set("dueDate")} />
      </Field>
      <Field id="task-priority" label="Priority">
        <select id="task-priority" className="input" value={input.priority}
          onChange={(event) => setInput({ ...input, priority: event.target.value as Priority })}>
          {PRIORITIES.map((priority) => <option key={priority} value={priority}>{PRIORITY_LABEL[priority]}</option>)}
        </select>
      </Field>
      <div className="actions">
        <button type="submit" className="btn btn-primary" disabled={action.busy}>{submitLabel}</button>
        <button type="button" className="btn" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}
