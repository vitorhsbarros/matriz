import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { Task } from "./api";
import { Assignees } from "./Assignees";
import { fakeApi } from "./testApi";

const task: Task = {
  id: "t1", projectId: "p1", title: "Create icons", description: null, dueDate: null, status: "TODO",
  priority: "MEDIUM", assignees: [], createdAt: "2026-11-02T10:00:00Z",
};
const eligible = {
  status: 200,
  body: {
    people: [{ userId: "u-ana", name: "Ana Silva" }, { userId: "u-rui", name: "Rui Costa" }],
    teams: [{ teamId: "t-design", name: "Design" }],
  },
};
const teamsList = {
  status: 200,
  body: [
    { id: "t-design", name: "Design", members: [] },
    { id: "t-jba", name: "JBA", members: [] },
  ],
};
const withAna = { ...task, assignees: [{ type: "person", userId: "u-ana", name: "Ana Silva" }] };
const withAnaAndDesign = {
  ...task,
  assignees: [{ type: "person", userId: "u-ana", name: "Ana Silva" }, { type: "team", teamId: "t-design", name: "Design" }],
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

test("assigning a person and a team shows what the API saved, and hides them from the choices (US10, US13)", async () => {
  const api = fakeApi({
    "GET /api/projects/p1/eligible-assignees": [eligible],
    "GET /api/teams": [teamsList],
    "POST /api/projects/p1/tasks/t1/assignees": [{ status: 200, body: withAna }, { status: 200, body: withAnaAndDesign }],
  });
  let current: Task = task;
  const { rerender } = render(<Assignees projectId="p1" task={current} onChange={(t) => { current = t; }} />);
  expect(await screen.findByText("Unassigned")).toBeDefined();

  fireEvent.change(await screen.findByLabelText("Add an assignee"), { target: { value: "person:u-ana" } });
  fireEvent.click(screen.getByRole("button", { name: "Assign" }));
  await vi.waitFor(() => expect(current.assignees).toHaveLength(1));
  rerender(<Assignees projectId="p1" task={current} onChange={(t) => { current = t; }} />);
  expect(within(screen.getByRole("list", { name: "Task assignees" })).getByText("Ana Silva")).toBeDefined();
  expect(screen.queryByRole("option", { name: "Ana Silva" })).toBeNull();

  fireEvent.change(screen.getByLabelText("Add an assignee"), { target: { value: "team:t-design" } });
  fireEvent.click(screen.getByRole("button", { name: "Assign" }));
  await vi.waitFor(() => expect(current.assignees).toHaveLength(2));
  rerender(<Assignees projectId="p1" task={current} onChange={(t) => { current = t; }} />);
  expect(screen.getByText("Design (team)")).toBeDefined();

  expect(api.to("POST", "/api/projects/p1/tasks/t1/assignees").map((request) => request.body)).toEqual([
    { type: "person", userId: "u-ana" }, { type: "team", teamId: "t-design" },
  ]);
});

test("a refusal is shown and nothing changes (US10 AC05)", async () => {
  fakeApi({
    "GET /api/projects/p1/eligible-assignees": [eligible],
    "GET /api/teams": [teamsList],
    "POST /api/projects/p1/tasks/t1/assignees": [
      { status: 422, body: { status: 422, detail: "The assignee must be a member of the project." } },
    ],
  });
  const onChange = vi.fn();
  render(<Assignees projectId="p1" task={task} onChange={onChange} />);

  fireEvent.change(await screen.findByLabelText("Add an assignee"), { target: { value: "person:u-rui" } });
  fireEvent.click(screen.getByRole("button", { name: "Assign" }));

  expect(await screen.findByText("The assignee must be a member of the project.")).toBeDefined();
  expect(onChange).not.toHaveBeenCalled();
});

test("removing an assignee (US10 AC03)", async () => {
  const api = fakeApi({
    "GET /api/projects/p1/eligible-assignees": [eligible],
    "GET /api/teams": [teamsList],
    "DELETE /api/projects/p1/tasks/t1/assignees/teams/t-design": [{ status: 200, body: withAna }],
  });
  const onChange = vi.fn();
  render(<Assignees projectId="p1" task={withAnaAndDesign as Task} onChange={onChange} />);

  fireEvent.click(await screen.findByRole("button", { name: "Remove Design (team)" }));

  await vi.waitFor(() => expect(onChange).toHaveBeenCalledWith(withAna));
  expect(api.to("DELETE", "/api/projects/p1/tasks/t1/assignees/teams/t-design")).toHaveLength(1);
});

test("a team with members outside the project is shown disabled, with the rule (US13 AC04)", async () => {
  fakeApi({ "GET /api/projects/p1/eligible-assignees": [eligible], "GET /api/teams": [teamsList] });
  render(<Assignees projectId="p1" task={task} onChange={() => {}} />);

  const outside = await screen.findByRole("option", { name: "JBA (team) (not all members are in the project)" });
  expect((outside as HTMLOptionElement).disabled).toBe(true);
  expect((screen.getByRole("option", { name: "Design (team)" }) as HTMLOptionElement).disabled).toBe(false);
  expect(screen.getByText("All team members must belong to the project.")).toBeDefined();
});
