import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { ProjectPage } from "./ProjectPage";
import { fakeApi } from "./testApi";

const noTasks = { status: 200, body: [] };

const website = { id: "p-website", name: "Website", description: "New company website", role: "MANAGER" };

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

test("the manager sees the project with Edit and Delete project", async () => {
  fakeApi({ "GET /api/projects/p-website": [{ status: 200, body: website }],
    "GET /api/projects/p-website/tasks": [noTasks] });
  render(<ProjectPage projectId="p-website" tab="board" onLeave={() => {}} />);

  expect(await screen.findByRole("heading", { name: "Website" })).toBeDefined();
  expect(screen.getByText("Manager")).toBeDefined();
  expect(screen.getByText("New company website")).toBeDefined();
  expect(screen.getByRole("button", { name: "Edit" })).toBeDefined();
  expect(screen.getByRole("button", { name: "Delete project" })).toBeDefined();
});

test("a member sees neither Edit nor Delete project (US14 AC04, US15 AC04)", async () => {
  fakeApi({ "GET /api/projects/p-website": [{ status: 200, body: { ...website, role: "MEMBER" } }],
    "GET /api/projects/p-website/tasks": [noTasks] });
  render(<ProjectPage projectId="p-website" tab="board" onLeave={() => {}} />);

  expect(await screen.findByText("Member")).toBeDefined();
  expect(screen.queryByRole("button", { name: "Edit" })).toBeNull();
  expect(screen.queryByRole("button", { name: "Delete project" })).toBeNull();
});

test("a project the user can't access shows the API's message (DoD03)", async () => {
  fakeApi({ "GET /api/projects/p-shop": [{ status: 404, body: { status: 404, detail: "Project not found." } }] });
  render(<ProjectPage projectId="p-shop" tab="board" onLeave={() => {}} />);

  expect((await screen.findByRole("alert")).textContent).toBe("Project not found.");
});

test("editing saves both fields and shows the API's message on failure (US14)", async () => {
  const api = fakeApi({
    "GET /api/projects/p-website": [{ status: 200, body: website }],
    "GET /api/projects/p-website/tasks": [noTasks],
    "PATCH /api/projects/p-website": [
      { status: 422, body: { status: 422, detail: "Project name is required." } },
      { status: 200, body: { ...website, name: "Company website", description: null } },
    ],
  });
  render(<ProjectPage projectId="p-website" tab="board" onLeave={() => {}} />);
  fireEvent.click(await screen.findByRole("button", { name: "Edit" }));
  expect((screen.getByLabelText("Name") as HTMLInputElement).value).toBe("Website");

  fireEvent.change(screen.getByLabelText("Name"), { target: { value: "" } });
  fireEvent.click(screen.getByRole("button", { name: "Save" }));
  expect(await screen.findByText("Project name is required.")).toBeDefined();

  fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Company website" } });
  fireEvent.change(screen.getByLabelText("Description (optional)"), { target: { value: "" } });
  fireEvent.click(screen.getByRole("button", { name: "Save" }));

  expect(await screen.findByRole("heading", { name: "Company website" })).toBeDefined();
  expect(api.to("PATCH", "/api/projects/p-website")[1].body).toEqual({ name: "Company website", description: "" });
});

test("deleting asks for confirmation, then leaves the project (US15 AC01, AC02)", async () => {
  const api = fakeApi({
    "GET /api/projects/p-website": [{ status: 200, body: website }],
    "GET /api/projects/p-website/tasks": [noTasks],
    "DELETE /api/projects/p-website": [{ status: 204 }],
  });
  const onLeave = vi.fn();
  render(<ProjectPage projectId="p-website" tab="board" onLeave={onLeave} />);

  fireEvent.click(await screen.findByRole("button", { name: "Delete project" }));
  const dialog = screen.getByRole("dialog", { name: "Confirm deletion" });
  expect(within(dialog).getByText(`Delete "Website" and all its tasks? This can't be undone.`)).toBeDefined();
  expect(api.to("DELETE", "/api/projects/p-website")).toHaveLength(0);

  fireEvent.click(within(dialog).getByRole("button", { name: "Delete project" }));

  await vi.waitFor(() => expect(onLeave).toHaveBeenCalled());
  expect(api.to("DELETE", "/api/projects/p-website")).toHaveLength(1);
});
