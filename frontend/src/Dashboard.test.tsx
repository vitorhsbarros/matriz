import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { Dashboard } from "./Dashboard";
import { fakeApi, noProjects, zero } from "./testApi";

const app = { id: "p-app", name: "App", role: "MEMBER", taskCounts: { toDo: 4, inProgress: 1, done: 7 } };
const website = { id: "p-website", name: "Website", role: "MANAGER", taskCounts: { toDo: 3, inProgress: 2, done: 5 } };
const webShop = { id: "p-shop", name: "Web shop", role: "MANAGER", taskCounts: zero };
const totals = { toDo: 7, inProgress: 3, done: 12 };
const both = { status: 200, body: { totals, projects: [app, website] } };

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

test("shows my projects with my role, their counts and the totals (US04 AC01, AC07)", async () => {
  fakeApi({ "GET /api/projects": [both] });
  render(<Dashboard />);

  const items = within(await screen.findByRole("list", { name: "Projects" })).getAllByRole("listitem");
  expect(items.map((item) => item.textContent)).toEqual([
    "App Member To do 4 · In progress 1 · Done 7 ",
    "Website Manager To do 3 · In progress 2 · Done 5 Delete",
  ]);
  expect(screen.getByText("All my projects").parentElement!.textContent)
    .toBe("All my projects To do 7 · In progress 3 · Done 12");
  expect(screen.getByRole("link", { name: "Website" }).getAttribute("href")).toBe("#/projects/p-website");
});

test("not a member of any project: a message and no totals (US04 AC03)", async () => {
  fakeApi({ "GET /api/projects": [noProjects] });
  render(<Dashboard />);

  expect(await screen.findByText("You are not a member of any project yet.")).toBeDefined();
  expect(screen.queryByText("All my projects")).toBeNull();
});

test("searching narrows the list and keeps the totals (US23 AC01, AC02, AC05)", async () => {
  const api = fakeApi({
    "GET /api/projects": [both],
    "GET /api/projects?name=web": [{ status: 200, body: { totals, projects: [webShop, website] } }],
    "GET /api/projects?name=Intranet": [{ status: 200, body: { totals, projects: [] } }],
  });
  render(<Dashboard />);
  await screen.findByRole("link", { name: "App" });

  fireEvent.change(screen.getByLabelText("Search projects"), { target: { value: "web" } });
  expect(await screen.findByRole("link", { name: "Web shop" })).toBeDefined();
  expect(screen.queryByRole("link", { name: "App" })).toBeNull();

  fireEvent.change(screen.getByLabelText("Search projects"), { target: { value: "Intranet" } });
  expect(await screen.findByText("No projects match your search.")).toBeDefined();
  expect(screen.getByText("All my projects").parentElement!.textContent).toContain("To do 7");
  expect(api.to("GET", "/api/projects?name=Intranet")).toHaveLength(1);
});

test("creating a project shows the API's message on failure, then the new project (US03)", async () => {
  const api = fakeApi({
    "GET /api/projects": [noProjects, { status: 200, body: { totals: zero, projects: [{ ...website, taskCounts: zero }] } }],
    "POST /api/projects": [
      { status: 422, body: { status: 422, detail: "Project name is required." } },
      { status: 201, body: { id: "p-website", name: "Website", description: "New company website", role: "MANAGER" } },
    ],
  });
  render(<Dashboard />);
  fireEvent.click(await screen.findByRole("button", { name: "New project" }));

  fireEvent.click(screen.getByRole("button", { name: "Create project" }));
  expect(await screen.findByText("Project name is required.")).toBeDefined();

  fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Website" } });
  fireEvent.change(screen.getByLabelText("Description (optional)"), { target: { value: "New company website" } });
  fireEvent.click(screen.getByRole("button", { name: "Create project" }));

  expect(await screen.findByRole("link", { name: "Website" })).toBeDefined();
  expect(api.to("POST", "/api/projects")[1].body).toEqual({ name: "Website", description: "New company website" });
});

test("deleting asks for confirmation first, and Cancel deletes nothing (US15 AC01, AC03)", async () => {
  const api = fakeApi({
    "GET /api/projects": [both, { status: 200, body: { totals: { toDo: 4, inProgress: 1, done: 7 }, projects: [app] } }],
    "DELETE /api/projects/p-website": [{ status: 204 }],
  });
  render(<Dashboard />);

  fireEvent.click(await screen.findByRole("button", { name: "Delete Website" }));
  const dialog = screen.getByRole("dialog", { name: "Confirm deletion" });
  expect(within(dialog).getByText(`Delete "Website" and all its tasks? This can't be undone.`)).toBeDefined();
  fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
  expect(screen.queryByRole("dialog")).toBeNull();
  expect(api.to("DELETE", "/api/projects/p-website")).toHaveLength(0);

  fireEvent.click(screen.getByRole("button", { name: "Delete Website" }));
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Delete project" }));

  expect(await screen.findByRole("link", { name: "App" })).toBeDefined();
  expect(screen.queryByRole("link", { name: "Website" })).toBeNull();
  expect(api.to("DELETE", "/api/projects/p-website")).toHaveLength(1);
});
