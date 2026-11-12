import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { TaskPage } from "./TaskPage";
import { ana, fakeApi } from "./testApi";

const project = { status: 200, body: { id: "p1", name: "Website", description: null, role: "MEMBER" } };
const createLogo = {
  id: "t2", projectId: "p1", title: "Create logo", description: "Use the new colours", dueDate: "2026-11-20",
  status: "IN_PROGRESS", priority: "HIGH", assignees: [], createdAt: "2026-11-02T10:00:00Z",
};
/** The task details also read the eligible assignees and the comments. */
const extras = {
  "GET /api/projects/p1/eligible-assignees": [{ status: 200, body: { people: [], teams: [] } }],
  "GET /api/teams": [{ status: 200, body: [] }],
  "GET /api/projects/p1/tasks/t2/comments": [{ status: 200, body: [] }],
  "GET /api/projects/p1/tasks/t3/comments": [{ status: 200, body: [] }],
};
const reviewCopy = { ...createLogo, id: "t3", title: "Review copy", description: null, dueDate: null, priority: "MEDIUM" };

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

test("shows every field of the task (US20 AC01)", async () => {
  fakeApi({ ...extras, "GET /api/projects/p1": [project], "GET /api/projects/p1/tasks/t2": [{ status: 200, body: createLogo }] });
  render(<TaskPage projectId="p1" taskId="t2" me={ana} onLeave={() => {}} />);

  expect(await screen.findByRole("heading", { name: "Create logo" })).toBeDefined();
  expect((screen.getByLabelText("Status") as HTMLSelectElement).value).toBe("IN_PROGRESS");
  expect(screen.getByText("High")).toBeDefined();
  expect(screen.getByText("2026-11-20")).toBeDefined();
  expect(screen.getByText("Use the new colours")).toBeDefined();
  expect(await screen.findByRole("link", { name: "Website" })).toBeDefined();
});

test("a task with only a title says what is missing (US20 AC04)", async () => {
  fakeApi({ ...extras, "GET /api/projects/p1": [project], "GET /api/projects/p1/tasks/t3": [{ status: 200, body: reviewCopy }] });
  render(<TaskPage projectId="p1" taskId="t3" me={ana} onLeave={() => {}} />);

  expect(await screen.findByText("No description")).toBeDefined();
  expect(screen.getByText("No due date")).toBeDefined();
  expect(screen.getByText("Unassigned")).toBeDefined();
});

test("a task deleted in the meantime shows the API's message (US20 AC03)", async () => {
  fakeApi({
    ...extras,
    "GET /api/projects/p1": [project],
    "GET /api/projects/p1/tasks/t2": [{ status: 404, body: { status: 404, detail: "Task not found." } }],
  });
  render(<TaskPage projectId="p1" taskId="t2" me={ana} onLeave={() => {}} />);

  expect((await screen.findByRole("alert")).textContent).toBe("Task not found.");
});

test("changing the status saves only the status (US07)", async () => {
  const api = fakeApi({
    ...extras,
    "GET /api/projects/p1": [project],
    "GET /api/projects/p1/tasks/t2": [{ status: 200, body: createLogo }],
    "PATCH /api/projects/p1/tasks/t2": [{ status: 200, body: { ...createLogo, status: "DONE" } }],
  });
  render(<TaskPage projectId="p1" taskId="t2" me={ana} onLeave={() => {}} />);

  fireEvent.change(await screen.findByLabelText("Status"), { target: { value: "DONE" } });

  await vi.waitFor(() => expect((screen.getByLabelText("Status") as HTMLSelectElement).value).toBe("DONE"));
  expect(api.to("PATCH", "/api/projects/p1/tasks/t2")[0].body).toEqual({ status: "DONE" });
});

test("editing sends every field, clears empty ones and shows the API's message (US08)", async () => {
  const api = fakeApi({
    ...extras,
    "GET /api/projects/p1": [project],
    "GET /api/projects/p1/tasks/t2": [{ status: 200, body: createLogo }],
    "PATCH /api/projects/p1/tasks/t2": [
      { status: 422, body: { status: 422, detail: "Due date cannot be in the past." } },
      { status: 200, body: { ...createLogo, title: "Create the logo", description: null, dueDate: null } },
    ],
  });
  render(<TaskPage projectId="p1" taskId="t2" me={ana} onLeave={() => {}} />);
  fireEvent.click(await screen.findByRole("button", { name: "Edit" }));
  expect((screen.getByLabelText("Due date (optional)") as HTMLInputElement).value).toBe("2026-11-20");

  fireEvent.click(screen.getByRole("button", { name: "Save" }));
  expect(await screen.findByText("Due date cannot be in the past.")).toBeDefined();

  fireEvent.change(screen.getByLabelText("Title"), { target: { value: "Create the logo" } });
  fireEvent.change(screen.getByLabelText("Description (optional)"), { target: { value: "" } });
  fireEvent.change(screen.getByLabelText("Due date (optional)"), { target: { value: "" } });
  fireEvent.click(screen.getByRole("button", { name: "Save" }));

  expect(await screen.findByRole("heading", { name: "Create the logo" })).toBeDefined();
  expect(api.to("PATCH", "/api/projects/p1/tasks/t2")[1].body)
    .toEqual({ title: "Create the logo", description: null, dueDate: null, priority: "HIGH" });
});

test("deleting asks for confirmation, then leaves the task (US16 AC01, AC02)", async () => {
  const api = fakeApi({
    ...extras,
    "GET /api/projects/p1": [project],
    "GET /api/projects/p1/tasks/t2": [{ status: 200, body: createLogo }],
    "DELETE /api/projects/p1/tasks/t2": [{ status: 204 }],
  });
  const onLeave = vi.fn();
  render(<TaskPage projectId="p1" taskId="t2" me={ana} onLeave={onLeave} />);

  fireEvent.click(await screen.findByRole("button", { name: "Delete task" }));
  const dialog = screen.getByRole("dialog", { name: "Confirm deletion" });
  expect(within(dialog).getByText(`Delete "Create logo"? This can't be undone.`)).toBeDefined();
  fireEvent.click(within(dialog).getByRole("button", { name: "Delete task" }));

  await vi.waitFor(() => expect(onLeave).toHaveBeenCalled());
  expect(api.to("DELETE", "/api/projects/p1/tasks/t2")).toHaveLength(1);
});
