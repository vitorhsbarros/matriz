import { DragEvent, useCallback, useEffect, useRef, useState } from "react";
import {
  ApiError, assigneesText, createTask, deleteTask, listTasks, PRIORITIES, PRIORITY_LABEL, Priority, Status,
  STATUS_LABEL, STATUSES, Task, TaskInput, updateTask,
} from "./api";
import { ConfirmDelete, deleteTaskMessage } from "./ConfirmDelete";
import { TaskForm } from "./TaskForm";
import { Empty, ErrorMessage, Loading, messageOf, PriorityPill, statusClass, useAction } from "./ui";

type Props = { projectId: string };

const taskAddress = (projectId: string, taskId: string) => `#/projects/${projectId}/tasks/${taskId}`;

/** Toggles one value in a filter. */
function toggle<T>(values: T[], value: T): T[] {
  return values.includes(value) ? values.filter((current) => current !== value) : [...values, value];
}

// The project's board (US06; wireframe screen 2): one column per status, with its count and its cards, newest
// first. New task (US05), ← → and drag and drop to change the status (US07), Delete on each card (US16), and the
// status and priority filters (US24). After every change the board is read again from the API, so the columns and
// counts are the server's, without reloading the page.
export function Board({ projectId }: Props) {
  const [tasks, setTasks] = useState<Task[] | null>(null);
  const [statuses, setStatuses] = useState<Status[]>([]);
  const [priorities, setPriorities] = useState<Priority[]>([]);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<Task | null>(null);
  const [error, setError] = useState<string | null>(null);
  const moving = useAction();
  const latest = useRef(0);

  const load = useCallback(async () => {
    const request = ++latest.current;
    try {
      const result = await listTasks(projectId, statuses, priorities);
      if (request === latest.current) {
        setTasks(result);
        setError(null);
      }
    } catch (failure) {
      if (request === latest.current && !(failure instanceof ApiError && failure.status === 401)) {
        setError(messageOf(failure));
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
    await moving.run(() => updateTask(projectId, task.id, { status }).then(() => undefined));
    await load();   // also after a refusal such as "Task not found.", so the board shows what is really there
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
    <section className="panel" aria-label="Board">
      {!creating && (
        <div className="actions">
          <button type="button" className="btn btn-primary" onClick={() => setCreating(true)}>New task</button>
        </div>
      )}

      {creating && <TaskForm title="New task" submitLabel="Create task" onSubmit={create} onCancel={() => setCreating(false)} />}

      <div className="filters">
        <fieldset>
          <legend className="label">Status</legend>
          {STATUSES.map((status) => (
            <label key={status} className="chip">
              <input type="checkbox" checked={statuses.includes(status)}
                onChange={() => setStatuses(toggle(statuses, status))} /> {STATUS_LABEL[status]}
            </label>
          ))}
        </fieldset>
        <fieldset>
          <legend className="label">Priority</legend>
          {PRIORITIES.map((priority) => (
            <label key={priority} className="chip">
              <input type="checkbox" checked={priorities.includes(priority)}
                onChange={() => setPriorities(toggle(priorities, priority))} /> {PRIORITY_LABEL[priority]}
            </label>
          ))}
        </fieldset>
        {filtering && (
          <button type="button" className="btn btn-quiet btn-small"
            onClick={() => { setStatuses([]); setPriorities([]); }}>Clear filters</button>
        )}
      </div>

      <ErrorMessage message={error ?? moving.error} />

      {deleting && (
        <ConfirmDelete message={deleteTaskMessage(deleting.title)} confirmLabel="Delete task"
          onConfirm={() => deleteTask(projectId, deleting.id)}
          onDeleted={() => { setDeleting(null); load(); }}
          onCancel={() => setDeleting(null)} />
      )}

      {!tasks && !error && <Loading />}
      {tasks && tasks.length === 0 && <Empty>{filtering ? "No tasks match the filters." : "No tasks yet."}</Empty>}

      {tasks && (
        <div className="board" style={{ ["--cols" as string]: columns.length }}>
          {columns.map((status) => {
            const inColumn = tasks.filter((task) => task.status === status);
            const index = STATUSES.indexOf(status);
            const previous = STATUSES[index - 1];
            const next = STATUSES[index + 1];
            return (
              <section key={status} className={`column column-${statusClass(status)}`} aria-label={STATUS_LABEL[status]}
                onDragOver={(event) => event.preventDefault()} onDrop={(event) => drop(event, status)}>
                <h2>{STATUS_LABEL[status]} <span className="count">{inColumn.length}</span></h2>
                <ul>
                  {inColumn.map((task) => (
                    <li key={task.id} className="card" draggable
                      onDragStart={(event) => event.dataTransfer.setData("text/plain", task.id)}>
                      <a className="card-title" href={taskAddress(projectId, task.id)}>{task.title}</a>
                      <span>{assigneesText(task.assignees)}</span>
                      <div className="card-meta">
                        <PriorityPill priority={task.priority} />
                        <span>{task.dueDate ? `Due ${task.dueDate}` : "No due date"}</span>
                      </div>
                      <div className="card-actions">
                        {previous && (
                          <button type="button" className="btn btn-small" aria-label={`Move to ${STATUS_LABEL[previous]}`}
                            disabled={moving.busy} onClick={() => move(task, previous)}>←</button>
                        )}
                        {next && (
                          <button type="button" className="btn btn-small" aria-label={`Move to ${STATUS_LABEL[next]}`}
                            disabled={moving.busy} onClick={() => move(task, next)}>→</button>
                        )}
                        <button type="button" className="btn btn-quiet btn-small push" aria-label={`Delete task ${task.title}`}
                          onClick={() => setDeleting(task)}>Delete</button>
                      </div>
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
