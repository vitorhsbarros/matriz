// Calls to the Matriz API (openapi.yaml). The session cookie is sent by the browser and can't be read here
// (HttpOnly, ADR-0002). The CSRF token is kept in memory only, never in localStorage, and is sent in the
// X-CSRF-Token header on every request that changes data (SEC07, DoD06).

export type User = { id: string; name: string; email: string };
export type Session = { user: User; csrfToken: string };

/** An error answer from the API, with the exact message of the acceptance criteria (DoD04). */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, detail: string) {
    super(detail);
    this.status = status;
  }
}

let csrfToken: string | null = null;
let onLoggedOut: () => void = () => {};

/**
 * Called when the API answers 401 to a logged-in request: there is no session, or it is no longer valid
 * (DoD02, US02 AC06). The application then shows the log-in screen with "Please log in to continue.".
 */
export function whenLoggedOut(handler: () => void) {
  onLoggedOut = handler;
}

async function call<T>(method: string, path: string, body?: unknown): Promise<T | undefined> {
  const headers: Record<string, string> = {};
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (method !== "GET" && csrfToken !== null) {
    headers["X-CSRF-Token"] = csrfToken;
  }
  const response = await fetch(`/api${path}`, {
    method,
    headers,
    credentials: "same-origin",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (response.status === 204) {
    return undefined;
  }
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new ApiError(response.status, typeof data.detail === "string" ? data.detail : "");
  }
  return data as T;
}

/** Any call other than log-in: a 401 means the session ended. */
async function callLoggedIn<T>(method: string, path: string, body?: unknown) {
  try {
    return await call<T>(method, path, body);
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      csrfToken = null;
      onLoggedOut();
    }
    throw error;
  }
}

export async function logIn(email: string, password: string): Promise<Session> {
  const session = (await call<Session>("POST", "/session", { email, password }))!;
  csrfToken = session.csrfToken;
  return session;
}

/** The current session after a page reload (US02 AC10), or null when nobody is logged in. */
export async function currentSession(): Promise<Session | null> {
  try {
    const session = (await call<Session>("GET", "/session"))!;
    csrfToken = session.csrfToken;
    return session;
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

export async function logOut(): Promise<void> {
  await callLoggedIn<void>("DELETE", "/session");
  csrfToken = null;
}

// ---------------------------------------------------------------- projects (US03, US04, US14, US15, US23)

export type Role = "MANAGER" | "MEMBER";
export type TaskCounts = { toDo: number; inProgress: number; done: number };
export type ProjectSummary = { id: string; name: string; role: Role; taskCounts: TaskCounts };
export type ProjectDashboard = { totals: TaskCounts; projects: ProjectSummary[] };
export type Project = { id: string; name: string; description: string | null; role: Role };

/** The home dashboard; `name` narrows the list, never the totals (US23, D14). */
export async function getDashboard(name = ""): Promise<ProjectDashboard> {
  const query = name.trim() === "" ? "" : `?name=${encodeURIComponent(name)}`;
  return (await callLoggedIn<ProjectDashboard>("GET", `/projects${query}`))!;
}

export async function createProject(name: string, description: string): Promise<Project> {
  return (await callLoggedIn<Project>("POST", "/projects", { name, description }))!;
}

export async function getProject(id: string): Promise<Project> {
  return (await callLoggedIn<Project>("GET", `/projects/${encodeURIComponent(id)}`))!;
}

/** Sends both fields; an empty description clears it (US14 AC02). */
export async function updateProject(id: string, name: string, description: string): Promise<Project> {
  return (await callLoggedIn<Project>("PATCH", `/projects/${encodeURIComponent(id)}`, { name, description }))!;
}

export async function deleteProject(id: string): Promise<void> {
  await callLoggedIn<void>("DELETE", `/projects/${encodeURIComponent(id)}`);
}

// ---------------------------------------------------------------- tasks (US05–US08, US16, US20, US24)

export type Status = "TODO" | "IN_PROGRESS" | "DONE";
export type Priority = "LOW" | "MEDIUM" | "HIGH";
export const STATUSES: Status[] = ["TODO", "IN_PROGRESS", "DONE"];
export const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH"];
export const STATUS_LABEL: Record<Status, string> = { TODO: "To do", IN_PROGRESS: "In progress", DONE: "Done" };
export const PRIORITY_LABEL: Record<Priority, string> = { LOW: "Low", MEDIUM: "Medium", HIGH: "High" };

export type Task = {
  id: string;
  projectId: string;
  title: string;
  description: string | null;
  dueDate: string | null;
  status: Status;
  priority: Priority;
  assignees: unknown[];
  createdAt: string;
};

/** The fields of the task form; an empty description or due date means none. */
export type TaskInput = { title: string; description: string; dueDate: string; priority: Priority };

const tasksPath = (projectId: string) => `/projects/${encodeURIComponent(projectId)}/tasks`;
const taskPath = (projectId: string, taskId: string) => `${tasksPath(projectId)}/${encodeURIComponent(taskId)}`;

/** The project's tasks, newest first; each filter may hold several values (US24, AS17). */
export async function listTasks(projectId: string, statuses: Status[] = [], priorities: Priority[] = []) {
  const query = new URLSearchParams();
  statuses.forEach((status) => query.append("status", status));
  priorities.forEach((priority) => query.append("priority", priority));
  const suffix = query.toString() === "" ? "" : `?${query}`;
  return (await callLoggedIn<Task[]>("GET", `${tasksPath(projectId)}${suffix}`))!;
}

export async function createTask(projectId: string, input: TaskInput): Promise<Task> {
  return (await callLoggedIn<Task>("POST", tasksPath(projectId), {
    title: input.title,
    description: input.description,
    dueDate: input.dueDate === "" ? null : input.dueDate,
    priority: input.priority,
  }))!;
}

export async function getTask(projectId: string, taskId: string): Promise<Task> {
  return (await callLoggedIn<Task>("GET", taskPath(projectId, taskId)))!;
}

/** Changes only the fields given; `null` clears the description or the due date (US08). */
export async function updateTask(projectId: string, taskId: string, fields: Record<string, string | null>) {
  return (await callLoggedIn<Task>("PATCH", taskPath(projectId, taskId), fields))!;
}

export async function deleteTask(projectId: string, taskId: string): Promise<void> {
  await callLoggedIn<void>("DELETE", taskPath(projectId, taskId));
}
