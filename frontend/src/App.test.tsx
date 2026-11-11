import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { App } from "./App";

// The API is replaced by a list of answers, one per request, in order. Made-up data only (PRI05).

type Answer = { status: number; body?: unknown };

const ana = { id: "6f1c0d3e-0000-4000-8000-000000000001", name: "Ana Silva", email: "ana@example.com" };
const session = { user: ana, csrfToken: "token-1" };
const notLoggedIn: Answer = { status: 401, body: { status: 401, detail: "Please log in to continue." } };

function api(...answers: Answer[]) {
  const fetchMock = vi.fn(async () => {
    const answer = answers.shift();
    if (!answer) {
      throw new Error("Unexpected request");
    }
    return new Response(answer.body === undefined ? null : JSON.stringify(answer.body), {
      status: answer.status,
      headers: { "Content-Type": "application/json" },
    });
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function request(fetchMock: ReturnType<typeof api>, index: number) {
  const [url, init] = fetchMock.mock.calls[index] as unknown as [string, RequestInit];
  return { url, method: init.method, headers: init.headers as Record<string, string>, body: init.body };
}

function fillIn(email: string, password: string) {
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: email } });
  fireEvent.change(screen.getByLabelText("Password"), { target: { value: password } });
  fireEvent.click(screen.getByRole("button", { name: "Log in" }));
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

test("a visitor is asked to log in (DoD02)", async () => {
  api(notLoggedIn);
  render(<App />);

  expect((await screen.findByRole("alert")).textContent).toBe("Please log in to continue.");
  expect(screen.getByRole("button", { name: "Log in" })).toBeDefined();
});

test("logging in shows the API's message on failure and the user's name on success (US02)", async () => {
  const fetchMock = api(
    notLoggedIn,
    { status: 401, body: { status: 401, detail: "Invalid email or password." } },
    { status: 200, body: session },
  );
  render(<App />);
  await screen.findByLabelText("Email");

  fillIn("ana@example.com", "wrong-password");
  expect(await screen.findByText("Invalid email or password.")).toBeDefined();

  fillIn("ana@example.com", "the-right-password");
  expect(await screen.findByText("Ana Silva")).toBeDefined();
  expect(request(fetchMock, 2)).toMatchObject({ url: "/api/session", method: "POST" });
  expect(JSON.parse(String(request(fetchMock, 2).body))).toEqual({
    email: "ana@example.com",
    password: "the-right-password",
  });
});

test("after a page reload the user is still logged in (US02 AC10)", async () => {
  api({ status: 200, body: session });
  render(<App />);

  expect(await screen.findByText("Ana Silva")).toBeDefined();
  expect(screen.getByRole("button", { name: "Log out" })).toBeDefined();
});

test("logging out sends the CSRF token and goes back to log-in (US02 AC02, DoD06)", async () => {
  const fetchMock = api({ status: 200, body: session }, { status: 204 });
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Log out" }));

  expect(await screen.findByRole("button", { name: "Log in" })).toBeDefined();
  expect(screen.queryByRole("alert")).toBeNull();
  expect(request(fetchMock, 1)).toMatchObject({ url: "/api/session", method: "DELETE" });
  expect(request(fetchMock, 1).headers["X-CSRF-Token"]).toBe("token-1");
});

test("a session that is no longer valid goes back to log-in with a message (DoD02)", async () => {
  api({ status: 200, body: session }, notLoggedIn);
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Log out" }));

  expect((await screen.findByRole("alert")).textContent).toBe("Please log in to continue.");
  expect(screen.getByRole("button", { name: "Log in" })).toBeDefined();
});
