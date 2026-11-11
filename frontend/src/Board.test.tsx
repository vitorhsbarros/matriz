import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { Board } from "./Board";
import { fakeApi } from "./testApi";

const task = (id: string, title: string, status: string, priority = "MEDIUM", dueDate: string | null = null) => ({
  id, projectId: "p1", title, description: null, dueDate, status, priority, assignees: [],
  createdAt: "2026-11-02T10:00:00Z",
});
const testTheSite = task("t1", "Test the site", "TODO");
const createLogo = task("t2", "Create logo", "IN_PROGRESS", "HIGH", "2026-11-20");
const reviewCopy = task("t3", "Review copy", "DONE", "LOW");
const all = { status: 200, body: [testTheSite, createLogo, reviewCopy] };

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

function column(name: string) {
  return screen.getByRole("region", { name });
}

test("shows one column per status with its count and cards (US06 AC01)", async () => {
  fakeApi({ "GET /api/projects/p1/tasks": [all] });
  render(<Board projectId="p1" />);

  expect(await screen.findByRole("heading", { name: "To do 1" })).toBeDefined();
  expect(screen.getAllByRole("heading", { level: 2 }).map((heading) => heading.textContent))
    .toEqual(["To do 1", "In progress 1", "Done 1"]);
  const card = within(column("In progress")).getByRole("listitem");
  expect(card.textContent).toContain("Create logo");
  expect(card.textContent).toContain("High");
  expect(card.textContent).toContain("Unassigned");
  expect(card.textContent).toContain("Due 2026-11-20");
  expect(within(column("To do")).getByRole("listitem").textContent).toContain("No due date");
  expect(screen.getByRole("link", { name: "Test the site" }).getAttribute("href")).toBe("#/projects/p1/tasks/t1");
});

test("a project without tasks shows the message and empty columns (US06 AC02)", async () => {
  fakeApi({ "GET /api/projects/p1/tasks": [{ status: 200, body: [] }] });
  render(<Board projectId="p1" />);

  expect(await screen.findByText("No tasks yet.")).toBeDefined();
  expect(screen.getAllByRole("heading", { level: 2 }).map((heading) => heading.textContent))
    .toEqual(["To do 0", "In progress 0", "Done 0"]);
});

test("creating a task shows the API's message, then the new task without a reload (US05)", async () => {
  const api = fakeApi({
    "GET /api/projects/p1/tasks": [{ status: 200, body: [] }, { status: 200, body: [task("t9", "Design home page", "TODO", "HIGH")] }],
    "POST /api/projects/p1/tasks": [
      { status: 422, body: { status: 422, detail: "Task title is required." } },
      { status: 201, body: task("t9", "Design home page", "TODO", "HIGH") },
    ],
  });
  render(<Board projectId="p1" />);
  fireEvent.click(await screen.findByRole("button", { name: "New task" }));

  fireEvent.click(screen.getByRole("button", { name: "Create task" }));
  expect(await screen.findByText("Task title is required.")).toBeDefined();

  fireEvent.change(screen.getByLabelText("Title"), { target: { value: "Design home page" } });
  fireEvent.change(screen.getByLabelText("Priority"), { target: { value: "HIGH" } });
  fireEvent.click(screen.getByRole("button", { name: "Create task" }));

  expect(await screen.findByRole("heading", { name: "To do 1" })).toBeDefined();
  expect(api.to("POST", "/api/projects/p1/tasks")[1].body)
    .toEqual({ title: "Design home page", description: "", dueDate: null, priority: "HIGH" });
});

test("the arrows change the status and the counts follow without a reload (US07)", async () => {
  const api = fakeApi({
    "GET /api/projects/p1/tasks": [all, { status: 200, body: [{ ...testTheSite, status: "IN_PROGRESS" }, createLogo, reviewCopy] }],
    "PATCH /api/projects/p1/tasks/t1": [{ status: 200, body: { ...testTheSite, status: "IN_PROGRESS" } }],
  });
  render(<Board projectId="p1" />);

  fireEvent.click(await within(await screen.findByRole("region", { name: "To do" }))
    .findByRole("button", { name: "Move to In progress" }));

  expect(await screen.findByRole("heading", { name: "In progress 2" })).toBeDefined();
  expect(screen.getByRole("heading", { name: "To do 0" })).toBeDefined();
  expect(api.to("PATCH", "/api/projects/p1/tasks/t1")[0].body).toEqual({ status: "IN_PROGRESS" });
});

test("the filters narrow the board, hide unchosen columns and can be cleared (US24)", async () => {
  const api = fakeApi({
    "GET /api/projects/p1/tasks": [all],
    "GET /api/projects/p1/tasks?priority=HIGH": [{ status: 200, body: [createLogo] }],
    "GET /api/projects/p1/tasks?status=DONE&priority=HIGH": [{ status: 200, body: [] }],
  });
  render(<Board projectId="p1" />);
  await screen.findByRole("link", { name: "Review copy" });

  fireEvent.click(within(screen.getByRole("group", { name: "Priority" })).getByLabelText("High"));
  expect(await screen.findByRole("heading", { name: "To do 0" })).toBeDefined();
  expect(screen.getByRole("heading", { name: "In progress 1" })).toBeDefined();
  expect(screen.queryByRole("link", { name: "Review copy" })).toBeNull();

  fireEvent.click(within(screen.getByRole("group", { name: "Status" })).getByLabelText("Done"));
  expect(await screen.findByText("No tasks match the filters.")).toBeDefined();
  expect(screen.getAllByRole("heading", { level: 2 }).map((heading) => heading.textContent)).toEqual(["Done 0"]);

  fireEvent.click(screen.getByRole("button", { name: "Clear filters" }));
  expect(await screen.findByRole("link", { name: "Review copy" })).toBeDefined();
  expect(api.to("GET", "/api/projects/p1/tasks")).toHaveLength(2);
});

test("deleting from a card asks for confirmation first (US16 AC01–AC03, AC05)", async () => {
  const api = fakeApi({
    "GET /api/projects/p1/tasks": [all, { status: 200, body: [createLogo, reviewCopy] }],
    "DELETE /api/projects/p1/tasks/t1": [
      { status: 404, body: { status: 404, detail: "Task not found." } },
      { status: 204 },
    ],
  });
  render(<Board projectId="p1" />);

  fireEvent.click(await screen.findByRole("button", { name: "Delete task Test the site" }));
  let dialog = screen.getByRole("dialog", { name: "Confirm deletion" });
  expect(within(dialog).getByText(`Delete "Test the site"? This can't be undone.`)).toBeDefined();
  fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
  expect(api.to("DELETE", "/api/projects/p1/tasks/t1")).toHaveLength(0);

  fireEvent.click(screen.getByRole("button", { name: "Delete task Test the site" }));
  dialog = screen.getByRole("dialog", { name: "Confirm deletion" });
  fireEvent.click(within(dialog).getByRole("button", { name: "Delete task" }));
  expect(await within(dialog).findByText("Task not found.")).toBeDefined();

  fireEvent.click(within(dialog).getByRole("button", { name: "Delete task" }));
  expect(await screen.findByRole("heading", { name: "To do 0" })).toBeDefined();
  expect(screen.queryByRole("link", { name: "Test the site" })).toBeNull();
});
