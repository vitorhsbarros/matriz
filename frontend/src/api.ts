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
