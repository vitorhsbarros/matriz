import { randomUUID } from "node:crypto";
import { defineConfig, devices } from "@playwright/test";

// End-to-end tests: a real browser against the application's JAR and a throw-away PostgreSQL
// (e2e/server.mjs). They run with `npm run e2e`, and in `./mvnw verify` after the JAR is built.

const port = process.env.E2E_PORT ?? "8095";
process.env.E2E_PORT = port;

// The demo account's password comes from the environment. Without one, this run makes up its own, which the
// application (through server.mjs) and the tests (through this process's environment) both receive.
process.env.MATRIZ_DEMO_PASSWORD_ANA ??= randomUUID();

export default defineConfig({
  testDir: "e2e",
  testMatch: "**/*.e2e.ts",
  forbidOnly: true,
  retries: 0,
  workers: 1,
  reporter: [["list"]],
  use: {
    baseURL: `http://localhost:${port}`,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: {
    command: "node e2e/server.mjs",
    url: `http://localhost:${port}/actuator/health`,
    reuseExistingServer: false,
    timeout: 120_000,
    gracefulShutdown: { signal: "SIGTERM", timeout: 10_000 },
    stdout: "ignore",
    stderr: "pipe",
  },
});
