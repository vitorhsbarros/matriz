import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { MembersPage } from "./MembersPage";
import { fakeApi, gate } from "./testApi";

const ana = { userId: "u-ana", name: "Ana Silva", email: "ana@example.com", role: "MANAGER" };
const rui = { userId: "u-rui", name: "Rui Costa", email: "rui@example.com", role: "MEMBER" };
const path = "/api/projects/p1/members";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

test("every member sees names, emails and roles, without the manager's controls (US09, PRI03)", async () => {
  fakeApi({ [`GET ${path}`]: [{ status: 200, body: [ana, rui] }] });
  render(<MembersPage projectId="p1" manager={false} />);

  const list = await screen.findByRole("list", { name: "Project members" });
  expect(within(list).getAllByRole("listitem").map((item) => item.textContent)).toEqual([
    "Ana Silva ana@example.comManager",
    "Rui Costa rui@example.comMember",
  ]);
  expect(screen.queryByRole("form", { name: "Add a member" })).toBeNull();
  expect(screen.queryByRole("button", { name: /Remove/ })).toBeNull();
});

test("the manager adds a member; a refusal keeps the email typed (US09 AC01, AC04)", async () => {
  const api = fakeApi({
    [`GET ${path}`]: [{ status: 200, body: [ana] }, { status: 200, body: [ana, rui] }],
    [`POST ${path}`]: [
      { status: 422, body: { status: 422, detail: "This user could not be added." } },
      { status: 201, body: rui },
    ],
  });
  render(<MembersPage projectId="p1" manager />);
  const email = await screen.findByLabelText("Email");

  fireEvent.change(email, { target: { value: "rui@example.com" } });
  fireEvent.click(screen.getByRole("button", { name: "Add member" }));
  expect(await screen.findByText("This user could not be added.")).toBeDefined();
  expect((email as HTMLInputElement).value).toBe("rui@example.com");

  fireEvent.click(screen.getByRole("button", { name: "Add member" }));
  expect(await screen.findByText("Rui Costa")).toBeDefined();
  expect((email as HTMLInputElement).value).toBe("");
  expect(api.to("POST", path).map((request) => request.body)).toEqual([
    { email: "rui@example.com" }, { email: "rui@example.com" },
  ]);
});

test("a double click adds only once (no duplicate request)", async () => {
  const slow = gate();
  const api = fakeApi({
    [`GET ${path}`]: [{ status: 200, body: [ana] }, { status: 200, body: [ana, rui] }],
    [`POST ${path}`]: [{ status: 201, body: rui, wait: slow.wait }],
  });
  render(<MembersPage projectId="p1" manager />);
  fireEvent.change(await screen.findByLabelText("Email"), { target: { value: "rui@example.com" } });

  const button = screen.getByRole("button", { name: "Add member" });
  fireEvent.click(button);
  fireEvent.click(button);
  slow.open();

  expect(await screen.findByText("Rui Costa")).toBeDefined();
  expect(api.to("POST", path)).toHaveLength(1);
});

test("the manager removes a member but not themself (US09 AC02, D07)", async () => {
  const api = fakeApi({
    [`GET ${path}`]: [{ status: 200, body: [ana, rui] }, { status: 200, body: [ana] }],
    [`DELETE ${path}/u-rui`]: [{ status: 204 }],
  });
  render(<MembersPage projectId="p1" manager />);

  expect(screen.queryByRole("button", { name: "Remove Ana Silva" })).toBeNull();
  fireEvent.click(await screen.findByRole("button", { name: "Remove Rui Costa" }));

  await vi.waitFor(() => expect(screen.queryByText("Rui Costa")).toBeNull());
  expect(api.to("DELETE", `${path}/u-rui`)).toHaveLength(1);
});

test("loading, then the API's refusal (DoD03)", async () => {
  const slow = gate();
  fakeApi({ [`GET ${path}`]: [{ status: 404, body: { status: 404, detail: "Project not found." }, wait: slow.wait }] });
  render(<MembersPage projectId="p1" manager={false} />);

  expect(screen.getByRole("status").textContent).toBe("Loading…");
  slow.open();
  expect((await screen.findByRole("alert")).textContent).toBe("Project not found.");
});
