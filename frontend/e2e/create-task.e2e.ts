import { randomUUID } from "node:crypto";
import { expect, test, type APIRequestContext } from "@playwright/test";

// A member's journey through the browser application: log in, open a project, create a task and find it on
// the project's board. Each run uses a project and a task title of its own.

const email = "ana@example.com";
const password = process.env.MATRIZ_DEMO_PASSWORD_ANA!;

let projectName: string;
let projectId: string;

async function logInThroughApi(request: APIRequestContext): Promise<string> {
  const session = await request.post("/api/session", { data: { email, password } });
  expect(session.status()).toBe(200);
  return (await session.json()).csrfToken;
}

test.beforeEach(async ({ request }) => {
  projectName = `Launch plan ${randomUUID().slice(0, 8)}`;
  const csrfToken = await logInThroughApi(request);
  const created = await request.post("/api/projects", {
    data: { name: projectName },
    headers: { "X-CSRF-Token": csrfToken },
  });
  expect(created.status()).toBe(201);
  projectId = (await created.json()).id;
});

test.afterEach(async ({ request }) => {
  const csrfToken = await logInThroughApi(request);
  const deleted = await request.delete(`/api/projects/${projectId}`, { headers: { "X-CSRF-Token": csrfToken } });
  expect(deleted.status()).toBe(204);
});

test("a member logs in, creates a task in a project and sees it on the board", async ({ page }) => {
  const title = `Write the release notes ${randomUUID().slice(0, 8)}`;

  await page.goto("/");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Password").fill(password);
  await page.getByRole("button", { name: "Log in" }).click();

  await expect(page.getByRole("heading", { name: "My projects", level: 1 })).toBeVisible();
  await page.getByRole("link", { name: projectName }).click();
  await expect(page.getByRole("heading", { name: projectName, level: 1 })).toBeVisible();

  const board = page.getByRole("region", { name: "Board" });
  await board.getByRole("button", { name: "New task" }).click();
  const form = board.getByRole("form", { name: "New task" });
  await form.getByRole("textbox", { name: "Title" }).fill(title);
  await form.getByRole("button", { name: "Create task" }).click();

  const toDo = board.getByRole("region", { name: "To do" });
  const card = toDo.getByRole("listitem").filter({ hasText: title });
  await expect(card).toBeVisible();
  await expect(card).toContainText("Unassigned");
  await expect(toDo.getByRole("heading", { level: 2 })).toHaveText(/^To do\s*1$/);

  for (const other of ["In progress", "Done"]) {
    await expect(board.getByRole("region", { name: other }).getByText(title)).toHaveCount(0);
  }
});
