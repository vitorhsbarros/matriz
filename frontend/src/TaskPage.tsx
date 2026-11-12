import { useEffect, useState } from "react";
import {
  ApiError, deleteTask, getProject, getTask, Status, STATUS_LABEL, STATUSES, Task, TaskInput, updateTask, User,
} from "./api";
import { Assignees } from "./Assignees";
import { Comments } from "./Comments";
import { ConfirmDelete, deleteTaskMessage } from "./ConfirmDelete";
import { TaskForm } from "./TaskForm";
import { Crumbs, ErrorMessage, Loading, messageOf, PriorityPill, useAction } from "./ui";

type Props = { projectId: string; taskId: string; me: User; onLeave: () => void };

// A task's details (US20; wireframe screen 3): every field, the status (US07), Edit (US08), Delete task (US16),
// the assignees (US10, US13) and the comments (US21, US22).
export function TaskPage({ projectId, taskId, me, onLeave }: Props) {
  const [task, setTask] = useState<Task | null>(null);
  const [projectName, setProjectName] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const statusChange = useAction();

  useEffect(() => {
    let current = true;
    setTask(null);
    setError(null);
    const report = (failure: unknown) => {
      if (current && !(failure instanceof ApiError && failure.status === 401)) {
        setError(messageOf(failure));   // "Project not found.", "Task not found."
      }
    };
    getProject(projectId).then((project) => current && setProjectName(project.name)).catch(report);
    getTask(projectId, taskId).then((result) => current && setTask(result)).catch(report);
    return () => { current = false; };
  }, [projectId, taskId]);

  function changeStatus(status: Status) {
    statusChange.run(async () => setTask(await updateTask(projectId, taskId, { status })));
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
    <>
      <Crumbs items={[
        { label: "My projects", href: "#" },
        { label: projectName ?? "Project", href: `#/projects/${projectId}` },
        { label: task?.title ?? "Task" },
      ]} />
      <ErrorMessage message={error} />
      {!task && !error && <Loading />}
      {task && (editing ? (
        <TaskForm title="Edit task" submitLabel="Save"
          initial={{ title: task.title, description: task.description ?? "", dueDate: task.dueDate ?? "", priority: task.priority }}
          onSubmit={save} onCancel={() => setEditing(false)} />
      ) : (
        <>
          <div className="page-head">
            <h1>{task.title}</h1>
            <div className="actions">
              <button type="button" className="btn" onClick={() => setEditing(true)}>Edit</button>
              <button type="button" className="btn btn-danger" onClick={() => setDeleting(true)}>Delete task</button>
            </div>
          </div>
          {deleting && (
            <ConfirmDelete message={deleteTaskMessage(task.title)} confirmLabel="Delete task"
              onConfirm={() => deleteTask(projectId, taskId)} onDeleted={onLeave} onCancel={() => setDeleting(false)} />
          )}
          <div className="detail">
            <div className="panel">
              <dl className="facts">
                <dt><label htmlFor="task-status">Status</label></dt>
                <dd>
                  <select id="task-status" className="input" value={task.status} disabled={statusChange.busy}
                    onChange={(event) => changeStatus(event.target.value as Status)}>
                    {STATUSES.map((status) => <option key={status} value={status}>{STATUS_LABEL[status]}</option>)}
                  </select>
                </dd>
                <dt>Priority</dt>
                <dd><PriorityPill priority={task.priority} /></dd>
                <dt>Due date</dt>
                <dd>{task.dueDate ?? "No due date"}</dd>
                <dt>Description</dt>
                <dd className="prose">{task.description ?? "No description"}</dd>
              </dl>
              <ErrorMessage message={statusChange.error} />
              <Comments projectId={projectId} taskId={taskId} me={me} />
            </div>
            <Assignees projectId={projectId} task={task} onChange={setTask} />
          </div>
        </>
      ))}
    </>
  );
}
