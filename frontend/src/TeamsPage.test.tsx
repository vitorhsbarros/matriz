import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { TeamsPage } from "./TeamsPage";
import { ana, fakeApi } from "./testApi";

const anaAsManager = { userId: ana.id, name: "Ana Silva", role: "MANAGER" };
const anaAsMember = { userId: ana.id, name: "Ana Silva", role: "MEMBER" };
const rui = { userId: "u-rui", name: "Rui Costa", role: "MEMBER" };
const ruiAsManager = { ...rui, role: "MANAGER" };
const design = { id: "t-design", name: "Design", members: [anaAsManager, rui] };
const qa = { id: "t-qa", name: "QA", members: [anaAsMember, ruiAsManager] };

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

test("every team with its members; the manager's controls only on the team Ana manages (US11, US12, D05)", async () => {
  fakeApi({ "GET /api/teams": [{ status: 200, body: [design, qa] }] });
  render(<TeamsPage me={ana} />);

  const designCard = await screen.findByRole("region", { name: "Team Design" });
  const qaCard = screen.getByRole("region", { name: "Team QA" });
  expect(within(designCard).getAllByRole("listitem").map((item) => item.textContent))
    .toEqual(["Ana SilvaManager", "Rui CostaMemberRemove"]);
  expect(within(designCard).getByRole("button", { name: "Rename" })).toBeDefined();
  expect(within(designCard).getByRole("button", { name: "Delete team" })).toBeDefined();
  expect(within(qaCard).queryByRole("button", { name: "Rename" })).toBeNull();
  expect(within(qaCard).queryByRole("button", { name: /Remove/ })).toBeNull();
});

test("there are no teams yet", async () => {
  fakeApi({ "GET /api/teams": [{ status: 200, body: [] }] });
  render(<TeamsPage me={ana} />);

  expect(await screen.findByText("No teams yet.")).toBeDefined();
});

test("creating a team shows the API's refusal and keeps the name, then lists the new team (US11 AC03)", async () => {
  const api = fakeApi({
    "GET /api/teams": [{ status: 200, body: [] }, { status: 200, body: [design] }],
    "POST /api/teams": [
      { status: 409, body: { status: 409, detail: "A team with this name already exists." } },
      { status: 201, body: design },
    ],
  });
  render(<TeamsPage me={ana} />);
  fireEvent.click(await screen.findByRole("button", { name: "New team" }));
  const name = screen.getByLabelText("Name");
  fireEvent.change(name, { target: { value: "Design" } });

  fireEvent.click(screen.getByRole("button", { name: "Create team" }));
  expect(await screen.findByText("A team with this name already exists.")).toBeDefined();
  expect((name as HTMLInputElement).value).toBe("Design");

  fireEvent.click(screen.getByRole("button", { name: "Create team" }));
  expect(await screen.findByRole("region", { name: "Team Design" })).toBeDefined();
  expect(api.to("POST", "/api/teams")).toHaveLength(2);
});

test("renaming, adding a member and deleting with the confirmation (US17, US12, US18)", async () => {
  const api = fakeApi({
    "GET /api/teams": [
      { status: 200, body: [design] },
      { status: 200, body: [{ ...design, name: "Product design" }] },
      { status: 200, body: [{ ...design, name: "Product design" }] },
      { status: 200, body: [] },
    ],
    "PATCH /api/teams/t-design": [{ status: 200, body: { ...design, name: "Product design" } }],
    "POST /api/teams/t-design/members": [{ status: 201, body: { userId: "u-sara", name: "Sara Lopes", role: "MEMBER" } }],
    "DELETE /api/teams/t-design": [{ status: 204 }],
  });
  render(<TeamsPage me={ana} />);

  fireEvent.click(await screen.findByRole("button", { name: "Rename" }));
  fireEvent.change(screen.getByLabelText("Name"), { target: { value: "Product design" } });
  fireEvent.click(screen.getByRole("button", { name: "Save" }));
  const card = await screen.findByRole("region", { name: "Team Product design" });

  fireEvent.change(within(card).getByLabelText("Email"), { target: { value: "sara@example.com" } });
  fireEvent.click(within(card).getByRole("button", { name: "Add member" }));
  await vi.waitFor(() => expect(api.to("POST", "/api/teams/t-design/members")).toHaveLength(1));

  fireEvent.click(within(screen.getByRole("region", { name: "Team Product design" })).getByRole("button", { name: "Delete team" }));
  const dialog = screen.getByRole("dialog", { name: "Confirm deletion" });
  expect(within(dialog).getByText(
    `Delete the team "Product design"? It will be removed from its tasks. This can't be undone.`)).toBeDefined();
  expect(api.to("DELETE", "/api/teams/t-design")).toHaveLength(0);
  fireEvent.click(within(dialog).getByRole("button", { name: "Delete team" }));

  expect(await screen.findByText("No teams yet.")).toBeDefined();
  expect(api.to("PATCH", "/api/teams/t-design")[0].body).toEqual({ name: "Product design" });
});
