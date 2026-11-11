import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { App } from "./App";
import { fakeApi, loggedIn, noProjects, notLoggedIn } from "./testApi";

function fillIn(email: string, password: string) {
  fireEvent.change(screen.getByLabelText("Email"), { target: { value: email } });
  fireEvent.change(screen.getByLabelText("Password"), { target: { value: password } });
  fireEvent.click(screen.getByRole("button", { name: "Log in" }));
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.location.hash = "";
});

test("a visitor is asked to log in (DoD02)", async () => {
  fakeApi({ "GET /api/session": [notLoggedIn] });
  render(<App />);

  expect((await screen.findByRole("alert")).textContent).toBe("Please log in to continue.");
  expect(screen.getByRole("button", { name: "Log in" })).toBeDefined();
});

test("logging in shows the API's message on failure, and the dashboard on success (US02 AC01)", async () => {
  const api = fakeApi({
    "GET /api/session": [notLoggedIn],
    "POST /api/session": [
      { status: 401, body: { status: 401, detail: "Invalid email or password." } },
      loggedIn,
    ],
    "GET /api/projects": [noProjects],
  });
  render(<App />);
  await screen.findByLabelText("Email");

  fillIn("ana@example.com", "wrong-password");
  expect(await screen.findByText("Invalid email or password.")).toBeDefined();

  fillIn("ana@example.com", "the-right-password");
  expect(await screen.findByText("Ana Silva")).toBeDefined();
  expect(await screen.findByRole("heading", { name: "My projects" })).toBeDefined();
  expect(api.to("POST", "/api/session")[1].body).toEqual({
    email: "ana@example.com",
    password: "the-right-password",
  });
});

test("after a page reload the user is still logged in (US02 AC10)", async () => {
  fakeApi({ "GET /api/session": [loggedIn], "GET /api/projects": [noProjects] });
  render(<App />);

  expect(await screen.findByText("Ana Silva")).toBeDefined();
  expect(screen.getByRole("button", { name: "Log out" })).toBeDefined();
});

test("logging out sends the CSRF token and goes back to log-in (US02 AC02, DoD06)", async () => {
  const api = fakeApi({
    "GET /api/session": [loggedIn],
    "GET /api/projects": [noProjects],
    "DELETE /api/session": [{ status: 204 }],
  });
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Log out" }));

  expect(await screen.findByRole("button", { name: "Log in" })).toBeDefined();
  expect(screen.queryByRole("alert")).toBeNull();
  expect(api.to("DELETE", "/api/session")[0].headers["X-CSRF-Token"]).toBe("token-1");
});

test("a session that is no longer valid goes back to log-in with a message (DoD02)", async () => {
  fakeApi({ "GET /api/session": [loggedIn], "GET /api/projects": [noProjects], "DELETE /api/session": [notLoggedIn] });
  render(<App />);

  fireEvent.click(await screen.findByRole("button", { name: "Log out" }));

  expect((await screen.findByRole("alert")).textContent).toBe("Please log in to continue.");
  expect(screen.getByRole("button", { name: "Log in" })).toBeDefined();
});

test("a reload on a project's address opens that project again", async () => {
  window.location.hash = "#/projects/p-website";
  fakeApi({
    "GET /api/session": [loggedIn],
    "GET /api/projects/p-website": [
      { status: 200, body: { id: "p-website", name: "Website", description: null, role: "MANAGER" } },
    ],
    "GET /api/projects/p-website/tasks": [{ status: 200, body: [] }],
  });
  render(<App />);

  expect(await screen.findByRole("heading", { name: "Website" })).toBeDefined();
});
