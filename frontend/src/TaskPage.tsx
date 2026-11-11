import { useEffect, useState } from "react";
import {
  ApiError, deleteTask, getProject, getTask, PRIORITY_LABEL, Status, STATUS_LABEL, STATUSES, Task, TaskInput,
  updateTask,
} from "./api";
import { ConfirmDelete, deleteTaskMessage } from "./ConfirmDelete";
import { TaskForm } from "./TaskForm";

type Props = { projectId: string; taskId: string; onLeave: () => void };

// A task's details (US20; wireframe screen 3, without assignees and comments, which arrive with US10, US13 and
// US21): every field, the status (US07), Edit (US08) and Delete task (US16).
export function TaskPage({ projectId, taskId, onLeave }: Props) {
  const [task, setTask] = useState<Task | null>(null);
  const [projectName, setProjectName] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    let current = true;
    setTask(null);
    setError(null);
    const report = (failure: unknown) => {
      if (current && !(failure instanceof ApiError && failure.status === 401)) {
        setError(failure instanceof Error ? failure.message : String(failure));   // "Project not found." ...
      }
    };
    getProject(projectId).then((project) => current && setProjectName(project.name)).catch(report);
    getTask(projectId, taskId).then((result) => current && setTask(result)).catch(report);
    return () => { current = false; };
  }, [projectId, taskId]);

  async function changeStatus(status: Status) {
    setError(null);
    try {
      setTask(await updateTask(projectId, taskId, { status }));
    } catch (failure) {
      if (!(failure instanceof ApiError && failure.status === 401)) {
        setError(failure instanceof Error ? failure.message : String(failure));
      }
    }
  }

  async function save(input: TaskInput) {
    setTask(await updateTask(projectId, taskId, {
      title: input.title,
      description: input.description === "" ? null : input.description,
      dueDate: input.dueDate === "" ? null : input.dueDate,
      priority: input.priority,
    }));
    setEditing(false);
  }

  return (
    <main>
      <p>
        <a href="#">My projects</a> / <a href={`#/projects/${projectId}`}>{projectName ?? "Project"}</a>
      </p>
      {error && <p role="alert">{error}</p>}
      {task && (editing ? (
        <TaskForm submitLabel="Save"
          initial={{
            title: task.title,
            description: task.description ?? "",
            dueDate: task.dueDate ?? "",
            priority: task.priority,
          }}
          onSubmit={save} onCancel={() => setEditing(false)} />
      ) : (
        <>
          <h1>{task.title}</h1>
          <p>
            <label htmlFor="task-status">Status</label>{" "}
            <select id="task-status" value={task.status}
              onChange={(event) => changeStatus(event.target.value as Status)}>
              {STATUSES.map((status) => <option key={status} value={status}>{STATUS_LABEL[status]}</option>)}
            </select>
          </p>
          <dl>
            <dt>Priority</dt>
            <dd>{PRIORITY_LABEL[task.priority]}</dd>
            <dt>Due date</dt>
            <dd>{task.dueDate ?? "No due date"}</dd>
            <dt>Assignees</dt>
            <dd>Unassigned</dd>
            <dt>Description</dt>
            <dd>{task.description ?? "No description"}</dd>
          </dl>
          <p>
            <button type="button" onClick={() => setEditing(true)}>Edit</button>{" "}
            <button type="button" onClick={() => setDeleting(true)}>Delete task</button>
          </p>
          {deleting && (
            <ConfirmDelete message={deleteTaskMessage(task.title)} confirmLabel="Delete task"
              onConfirm={() => deleteTask(projectId, taskId)} onDeleted={onLeave}
              onCancel={() => setDeleting(false)} />
          )}
        </>
      ))}
    </main>
  );
}
