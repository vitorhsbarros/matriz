import { DragEvent, useCallback, useEffect, useRef, useState } from "react";
import {
  ApiError, createTask, deleteTask, listTasks, PRIORITIES, PRIORITY_LABEL, Priority, Status, STATUS_LABEL, STATUSES,
  Task, TaskInput, updateTask,
} from "./api";
import { ConfirmDelete, deleteTaskMessage } from "./ConfirmDelete";
import { TaskForm } from "./TaskForm";

type Props = { projectId: string };

const taskAddress = (projectId: string, taskId: string) => `#/projects/${projectId}/tasks/${taskId}`;

/** Toggles one value in a filter. */
function toggle<T>(values: T[], value: T): T[] {
  return values.includes(value) ? values.filter((current) => current !== value) : [...values, value];
}

// The project's board (US06; wireframe screen 2): one column per status, with its count and its cards, newest
// first. New task (US05), ← → and drag and drop to change the status (US07), Delete on each card (US16), and
// the status and priority filters (US24). After every change the board is read again from the API, so the
// columns and counts are right without reloading the page.
export function Board({ projectId }: Props) {
  const [tasks, setTasks] = useState<Task[] | null>(null);
  const [statuses, setStatuses] = useState<Status[]>([]);
  const [priorities, setPriorities] = useState<Priority[]>([]);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<Task | null>(null);
  const [error, setError] = useState<string | null>(null);
  const latest = useRef(0);

  const load = useCallback(async () => {
    const request = ++latest.current;
    try {
      const result = await listTasks(projectId, statuses, priorities);
      if (request === latest.current) {
        setTasks(result);
      }
    } catch (failure) {
      if (request === latest.current && !(failure instanceof ApiError && failure.status === 401)) {
        setError(failure instanceof Error ? failure.message : String(failure));
      }
    }
  }, [projectId, statuses, priorities]);

  useEffect(() => {
    load();
  }, [load]);

  async function create(input: TaskInput) {
    await createTask(projectId, input);
    setCreating(false);
    await load();
  }

  async function move(task: Task, status: Status) {
    if (status === task.status) {
      return;
    }
    setError(null);
    try {
      await updateTask(projectId, task.id, { status });
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 401) {
        return;
      }
      setError(failure instanceof Error ? failure.message : String(failure));   // e.g. "Task not found."
    }
    await load();
  }

  function drop(event: DragEvent, status: Status) {
    event.preventDefault();
    const task = tasks?.find((candidate) => candidate.id === event.dataTransfer.getData("text/plain"));
    if (task) {
      move(task, status);
    }
  }

  const filtering = statuses.length > 0 || priorities.length > 0;
  const columns = statuses.length > 0 ? STATUSES.filter((status) => statuses.includes(status)) : STATUSES;

  return (
    <section aria-label="Board">
      {error && <p role="alert">{error}</p>}

      {creating ? (
        <TaskForm submitLabel="Create task" onSubmit={create} onCancel={() => setCreating(false)} />
      ) : (
        <button type="button" onClick={() => setCreating(true)}>New task</button>
      )}

      <fieldset>
        <legend>Status</legend>
        {STATUSES.map((status) => (
          <label key={status}>
            <input type="checkbox" checked={statuses.includes(status)}
              onChange={() => setStatuses(toggle(statuses, status))} /> {STATUS_LABEL[status]}{" "}
          </label>
        ))}
      </fieldset>
      <fieldset>
        <legend>Priority</legend>
        {PRIORITIES.map((priority) => (
          <label key={priority}>
            <input type="checkbox" checked={priorities.includes(priority)}
              onChange={() => setPriorities(toggle(priorities, priority))} /> {PRIORITY_LABEL[priority]}{" "}
          </label>
        ))}
      </fieldset>
      {filtering && (
        <button type="button" onClick={() => { setStatuses([]); setPriorities([]); }}>Clear filters</button>
      )}

      {deleting && (
        <ConfirmDelete message={deleteTaskMessage(deleting.title)} confirmLabel="Delete task"
          onConfirm={() => deleteTask(projectId, deleting.id)}
          onDeleted={() => { setDeleting(null); load(); }}
          onCancel={() => setDeleting(null)} />
      )}

      {tasks && tasks.length === 0 && <p>{filtering ? "No tasks match the filters." : "No tasks yet."}</p>}

      {tasks && (
        <div className="columns">
          {columns.map((status) => {
            const inColumn = tasks.filter((task) => task.status === status);
            return (
              <section key={status} aria-label={STATUS_LABEL[status]}
                onDragOver={(event) => event.preventDefault()} onDrop={(event) => drop(event, status)}>
                <h2>{STATUS_LABEL[status]} {inColumn.length}</h2>
                <ul>
                  {inColumn.map((task) => (
                    <li key={task.id} draggable
                      onDragStart={(event) => event.dataTransfer.setData("text/plain", task.id)}>
                      <a href={taskAddress(projectId, task.id)}>{task.title}</a>{" "}
                      <span>{PRIORITY_LABEL[task.priority]}</span>{" "}
                      <span>Unassigned</span>{" "}
                      <span>{task.dueDate ? `Due ${task.dueDate}` : "No due date"}</span>{" "}
                      {STATUSES.indexOf(status) > 0 && (
                        <button type="button" aria-label={`Move to ${STATUS_LABEL[STATUSES[STATUSES.indexOf(status) - 1]]}`}
                          onClick={() => move(task, STATUSES[STATUSES.indexOf(status) - 1])}>←</button>
                      )}
                      {STATUSES.indexOf(status) < STATUSES.length - 1 && (
                        <button type="button" aria-label={`Move to ${STATUS_LABEL[STATUSES[STATUSES.indexOf(status) + 1]]}`}
                          onClick={() => move(task, STATUSES[STATUSES.indexOf(status) + 1])}>→</button>
                      )}{" "}
                      <button type="button" aria-label={`Delete task ${task.title}`}
                        onClick={() => setDeleting(task)}>Delete</button>
                    </li>
                  ))}
                </ul>
              </section>
            );
          })}
        </div>
      )}
    </section>
  );
}
