import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import { Comments } from "./Comments";
import { ana, fakeApi, gate } from "./testApi";

const path = "/api/projects/p1/tasks/t1/comments";
const mine = {
  id: "c1", author: { userId: ana.id, name: "Ana Silva" }, text: "First draft is ready",
  createdAt: "2026-11-10T14:30:00", editedAt: null,
};
const ruis = {
  id: "c2", author: { userId: "u-rui", name: "Rui Costa" }, text: "Looks good",
  createdAt: "2026-11-10T15:02:00", editedAt: "2026-11-10T15:10:00",
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

test("comments oldest first, with author, date and time, and \"(edited)\" (US21 AC01, AC02, US22 AC01)", async () => {
  fakeApi({ [`GET ${path}`]: [{ status: 200, body: [mine, ruis] }] });
  render(<Comments projectId="p1" taskId="t1" me={ana} />);

  const first = await screen.findByRole("article", { name: "Comment by Ana Silva" });
  const second = screen.getByRole("article", { name: "Comment by Rui Costa" });
  expect(first.textContent).toContain("Ana Silva · 2026-11-10 14:30");
  expect(second.textContent).toContain("Rui Costa · 2026-11-10 15:02 (edited)");
  expect(within(first).getByRole("button", { name: "Edit" })).toBeDefined();
  expect(within(second).queryByRole("button", { name: "Edit" })).toBeNull();      // SEC11
  expect(within(second).queryByRole("button", { name: "Delete" })).toBeNull();
});

test("no comments yet (US21 AC07)", async () => {
  fakeApi({ [`GET ${path}`]: [{ status: 200, body: [] }] });
  render(<Comments projectId="p1" taskId="t1" me={ana} />);

  expect(await screen.findByText("No comments yet.")).toBeDefined();
});

test("adding keeps the text on a refusal, and sends only once on a double click (US21 AC03)", async () => {
  const slow = gate();
  const api = fakeApi({
    [`GET ${path}`]: [{ status: 200, body: [] }, { status: 200, body: [mine] }],
    [`POST ${path}`]: [
      { status: 422, body: { status: 422, detail: "Comment must be at most 2000 characters." } },
      { status: 201, body: mine, wait: slow.wait },
    ],
  });
  render(<Comments projectId="p1" taskId="t1" me={ana} />);
  const box = await screen.findByLabelText("Add a comment");
  fireEvent.change(box, { target: { value: "First draft is ready" } });

  fireEvent.click(screen.getByRole("button", { name: "Comment" }));
  expect(await screen.findByText("Comment must be at most 2000 characters.")).toBeDefined();
  expect((box as HTMLTextAreaElement).value).toBe("First draft is ready");

  fireEvent.click(screen.getByRole("button", { name: "Comment" }));
  fireEvent.click(screen.getByRole("button", { name: "Comment" }));
  slow.open();
  expect(await screen.findByRole("article", { name: "Comment by Ana Silva" })).toBeDefined();
  expect((box as HTMLTextAreaElement).value).toBe("");
  expect(api.to("POST", path)).toHaveLength(2);
});

test("editing and deleting my comment, with the confirmation (US22 AC01–AC04)", async () => {
  const api = fakeApi({
    [`GET ${path}`]: [
      { status: 200, body: [mine] },
      { status: 200, body: [{ ...mine, text: "Second draft is ready", editedAt: "2026-11-10T16:00:00" }] },
      { status: 200, body: [] },
    ],
    [`PATCH ${path}/c1`]: [{ status: 200, body: { ...mine, text: "Second draft is ready" } }],
    [`DELETE ${path}/c1`]: [{ status: 204 }],
  });
  render(<Comments projectId="p1" taskId="t1" me={ana} />);

  fireEvent.click(await screen.findByRole("button", { name: "Edit" }));
  fireEvent.change(screen.getByLabelText("Comment"), { target: { value: "Second draft is ready" } });
  fireEvent.click(screen.getByRole("button", { name: "Save" }));
  expect(await screen.findByText("Second draft is ready")).toBeDefined();
  expect(screen.getByRole("article").textContent).toContain("(edited)");

  fireEvent.click(screen.getByRole("button", { name: "Delete" }));
  const dialog = screen.getByRole("dialog", { name: "Confirm deletion" });
  expect(within(dialog).getByText("Delete this comment? This can't be undone.")).toBeDefined();
  fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
  expect(api.to("DELETE", `${path}/c1`)).toHaveLength(0);

  fireEvent.click(screen.getByRole("button", { name: "Delete" }));
  fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Delete comment" }));
  expect(await screen.findByText("No comments yet.")).toBeDefined();
  expect(api.to("PATCH", `${path}/c1`)[0].body).toEqual({ text: "Second draft is ready" });
});
