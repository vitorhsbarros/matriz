import { vi } from "vitest";

// A stand-in for the API in the browser application's tests: answers are given per "METHOD /path", and each
// path's answers are used in order (the last one repeats). Made-up data only (PRI05).

export type Answer = { status: number; body?: unknown };
export type Sent = { method: string; url: string; headers: Record<string, string>; body: unknown };

export const notLoggedIn: Answer = { status: 401, body: { status: 401, detail: "Please log in to continue." } };

export function fakeApi(answers: Record<string, Answer[]>) {
  const sent: Sent[] = [];
  vi.stubGlobal("fetch", vi.fn(async (url: string, init: RequestInit) => {
    const method = init.method ?? "GET";
    sent.push({
      method,
      url,
      headers: (init.headers ?? {}) as Record<string, string>,
      body: init.body ? JSON.parse(String(init.body)) : undefined,
    });
    const queue = answers[`${method} ${url}`];
    if (!queue || queue.length === 0) {
      throw new Error(`Unexpected request: ${method} ${url}`);
    }
    const answer = queue.length > 1 ? queue.shift()! : queue[0];
    return new Response(answer.body === undefined ? null : JSON.stringify(answer.body), {
      status: answer.status,
      headers: { "Content-Type": "application/json" },
    });
  }));
  return {
    sent,
    /** The requests sent with this method and URL. */
    to: (method: string, url: string) => sent.filter((request) => request.method === method && request.url === url),
  };
}

export const ana = { id: "6f1c0d3e-0000-4000-8000-000000000001", name: "Ana Silva", email: "ana@example.com" };
export const anaSession = { user: ana, csrfToken: "token-1" };
export const loggedIn: Answer = { status: 200, body: anaSession };

export const zero = { toDo: 0, inProgress: 0, done: 0 };
export const noProjects: Answer = { status: 200, body: { totals: zero, projects: [] } };
