// Starts what the end-to-end tests run against, and stops it again: a throw-away PostgreSQL in Docker and the
// application's JAR. Playwright starts this script (playwright.config.ts, webServer) and waits for the health
// check; at the end it sends SIGTERM, and the script stops the application and removes the container.
//
// The database is new on every run, so the tests never depend on data from an earlier one. The application
// runs in an empty working directory, so a developer's .env is never read.

import { execFileSync, spawn } from "node:child_process";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { randomUUID } from "node:crypto";

const LABEL = "matriz-e2e";
const jar = resolve(import.meta.dirname, "../../target/matriz-0.1.0-SNAPSHOT.jar");
const port = process.env.E2E_PORT ?? "8095";
const dbPassword = randomUUID();
let app;
let workDir;
let stopping = false;

const docker = (...args) => execFileSync("docker", args, { encoding: "utf8" }).trim();

// Containers left behind by a run that was killed before it could clean up.
for (const id of docker("ps", "-aq", "--filter", `label=${LABEL}`).split("\n").filter(Boolean)) {
  docker("rm", "-f", id);
}

const container = docker("run", "-d", "--rm", "--label", LABEL,
  "-e", "POSTGRES_DB=matriz", "-e", "POSTGRES_USER=matriz", "-e", `POSTGRES_PASSWORD=${dbPassword}`,
  "-p", "127.0.0.1::5432", "postgres:17.10");
const dbPort = docker("port", container, "5432/tcp").split("\n")[0].split(":").pop();

// The image first runs a temporary server that listens only on its socket, so only an answer over TCP
// means the real server is up.
const deadline = Date.now() + 60_000;
for (;;) {
  try {
    docker("exec", container, "pg_isready", "-h", "127.0.0.1", "-U", "matriz", "-d", "matriz");
    break;
  } catch (notYet) {
    if (Date.now() > deadline) {
      stop(1, "PostgreSQL did not start");
    }
    await new Promise((done) => setTimeout(done, 500));
  }
}

workDir = mkdtempSync(join(tmpdir(), "matriz-e2e-"));
app = spawn("java", ["-jar", jar], {
  cwd: workDir,
  stdio: "inherit",
  env: {
    ...process.env,
    SERVER_PORT: port,
    MATRIZ_DB_URL: `jdbc:postgresql://127.0.0.1:${dbPort}/matriz`,
    MATRIZ_DB_USER: "matriz",
    MATRIZ_DB_PASSWORD: dbPassword,
    MATRIZ_DEMO_SEED: "true",
    MATRIZ_COOKIE_SECURE: "false",
    MATRIZ_DEMO_PASSWORD_RUI: randomUUID(),
    MATRIZ_DEMO_PASSWORD_SARA: randomUUID(),
  },
});
app.on("exit", (code) => stop(code ?? 1, code ? `The application stopped with ${code}` : undefined));

for (const signal of ["SIGTERM", "SIGINT"]) {
  process.on(signal, () => stop(0));
}

// If Playwright is killed without stopping this script (e.g. kill -9), nothing would stop the application and
// the next run would find port 8095 taken. So the script also stops when the process that started it is gone.
const parent = process.ppid;
setInterval(() => {
  if (process.ppid !== parent) {
    stop(0, "Playwright is gone; stopping");
  }
}, 1000).unref();

async function stop(code, reason) {
  if (stopping) {
    return;
  }
  stopping = true;
  if (reason) {
    console.error(`[e2e] ${reason}`);
  }
  if (app && app.exitCode === null && app.signalCode === null) {
    // Wait for the application to stop, so the port is free when the next run starts.
    app.removeAllListeners("exit");
    const exited = new Promise((done) => app.once("exit", done));
    app.kill("SIGTERM");
    const timeout = new Promise((done) => setTimeout(done, 20_000));
    if (await Promise.race([exited.then(() => true), timeout.then(() => false)]) === false) {
      app.kill("SIGKILL");
    }
  }
  try {
    docker("rm", "-f", container);
  } catch (alreadyGone) {
    // --rm may have removed it already
  }
  if (workDir) {
    rmSync(workDir, { recursive: true, force: true });
  }
  process.exit(code);
}
