// Calls to the Matriz API (openapi.yaml). The session cookie is sent by the browser and can't be read here
// (HttpOnly, ADR-0002). The CSRF token is kept in memory only, never in localStorage, and is sent in the
// X-CSRF-Token header on every request that changes data (SEC07, DoD06). The API's address is set in config.ts.

import { API_BASE } from "./config";

export type User = { id: string; name: string; email: string };
export type Session = { user: User; csrfToken: string };

/**
 * Shown when the request gets no answer the stories describe: the server can't be reached, or it failed
 * without a message. Every refusal the stories define comes with its own exact message from the API (DoD04).
 */
export const REQUEST_FAILED = "The request could not be completed. Please try again.";

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
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      credentials: "same-origin",
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, REQUEST_FAILED);   // no answer at all
  }
  if (response.status === 204) {
    return undefined;
  }
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new ApiError(response.status, typeof data.detail === "string" && data.detail ? data.detail : REQUEST_FAILED);
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

/** A person or a team responsible for a task (US10, US13); the board shows a team as "<name> (team)". */
export type Assignee =
  | { type: "person"; userId: string; name: string }
  | { type: "team"; teamId: string; name: string };

export const assigneeLabel = (assignee: Assignee) =>
  assignee.type === "team" ? `${assignee.name} (team)` : assignee.name;

/** "Unassigned" when there is nobody (US06). */
export const assigneesText = (assignees: Assignee[]) =>
  assignees.length === 0 ? "Unassigned" : assignees.map(assigneeLabel).join(", ");

export type Task = {
  id: string;
  projectId: string;
  title: string;
  description: string | null;
  dueDate: string | null;
  status: Status;
  priority: Priority;
  assignees: Assignee[];
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

// ---------------------------------------------------------------- members (US09)

export type Member = { userId: string; name: string; email: string; role: Role };

export async function listMembers(projectId: string): Promise<Member[]> {
  return (await callLoggedIn<Member[]>("GET", `/projects/${encodeURIComponent(projectId)}/members`))!;
}

export async function addMember(projectId: string, email: string): Promise<Member> {
  return (await callLoggedIn<Member>("POST", `/projects/${encodeURIComponent(projectId)}/members`, { email }))!;
}

export async function removeMember(projectId: string, userId: string): Promise<void> {
  await callLoggedIn<void>("DELETE",
    `/projects/${encodeURIComponent(projectId)}/members/${encodeURIComponent(userId)}`);
}

// ---------------------------------------------------------------- teams (US11, US12, US17, US18)

export type TeamMember = { userId: string; name: string; role: Role };
export type Team = { id: string; name: string; members: TeamMember[] };

export async function listTeams(): Promise<Team[]> {
  return (await callLoggedIn<Team[]>("GET", "/teams"))!;
}

export async function createTeam(name: string): Promise<Team> {
  return (await callLoggedIn<Team>("POST", "/teams", { name }))!;
}

export async function renameTeam(teamId: string, name: string): Promise<Team> {
  return (await callLoggedIn<Team>("PATCH", `/teams/${encodeURIComponent(teamId)}`, { name }))!;
}

export async function deleteTeam(teamId: string): Promise<void> {
  await callLoggedIn<void>("DELETE", `/teams/${encodeURIComponent(teamId)}`);
}

export async function addTeamMember(teamId: string, email: string): Promise<TeamMember> {
  return (await callLoggedIn<TeamMember>("POST", `/teams/${encodeURIComponent(teamId)}/members`, { email }))!;
}

export async function removeTeamMember(teamId: string, userId: string): Promise<void> {
  await callLoggedIn<void>("DELETE", `/teams/${encodeURIComponent(teamId)}/members/${encodeURIComponent(userId)}`);
}

// ---------------------------------------------------------------- assignees (US10, US13)

export type EligibleAssignees = {
  people: { userId: string; name: string }[];
  teams: { teamId: string; name: string }[];
};

export async function listEligibleAssignees(projectId: string): Promise<EligibleAssignees> {
  return (await callLoggedIn<EligibleAssignees>("GET", `/projects/${encodeURIComponent(projectId)}/eligible-assignees`))!;
}

/** Adds one person or team; the answer is the whole task with its assignees. */
export async function addAssignee(projectId: string, taskId: string, assignee: { type: "person"; userId: string } | { type: "team"; teamId: string }) {
  return (await callLoggedIn<Task>("POST", `${taskPath(projectId, taskId)}/assignees`, assignee))!;
}

export async function removeAssignee(projectId: string, taskId: string, assignee: Assignee): Promise<Task> {
  const suffix = assignee.type === "person"
    ? `users/${encodeURIComponent(assignee.userId)}`
    : `teams/${encodeURIComponent(assignee.teamId)}`;
  return (await callLoggedIn<Task>("DELETE", `${taskPath(projectId, taskId)}/assignees/${suffix}`))!;
}

// ---------------------------------------------------------------- comments (US21, US22)

export type Comment = {
  id: string;
  author: { userId: string; name: string };
  text: string;
  createdAt: string;
  editedAt: string | null;
};

const commentsPath = (projectId: string, taskId: string) => `${taskPath(projectId, taskId)}/comments`;

export async function listComments(projectId: string, taskId: string): Promise<Comment[]> {
  return (await callLoggedIn<Comment[]>("GET", commentsPath(projectId, taskId)))!;
}

export async function addComment(projectId: string, taskId: string, text: string): Promise<Comment> {
  return (await callLoggedIn<Comment>("POST", commentsPath(projectId, taskId), { text }))!;
}

export async function editComment(projectId: string, taskId: string, commentId: string, text: string) {
  return (await callLoggedIn<Comment>("PATCH", `${commentsPath(projectId, taskId)}/${encodeURIComponent(commentId)}`, { text }))!;
}

export async function deleteComment(projectId: string, taskId: string, commentId: string): Promise<void> {
  await callLoggedIn<void>("DELETE", `${commentsPath(projectId, taskId)}/${encodeURIComponent(commentId)}`);
}
