import { ReactNode, useCallback, useRef, useState } from "react";
import { ApiError, REQUEST_FAILED, Priority, PRIORITY_LABEL, Status, STATUS_LABEL } from "./api";
import { applyTheme, savedTheme, Theme } from "./theme";

/** The message to show for a failed request: the API's exact message (DoD04), or the generic one. */
export function messageOf(failure: unknown): string {
  return failure instanceof Error && failure.message ? failure.message : REQUEST_FAILED;
}

/**
 * Runs one request at a time: while it is in progress `busy` is true and a second `run` does nothing, so a
 * double click never sends the request twice. A failure leaves its message in `error`. A 401 shows nothing here,
 * because the application is already back on the log-in screen (DoD02), except on log-in itself, where it means
 * "Invalid email or password." (`showUnauthorized`).
 */
export function useAction({ showUnauthorized = false } = {}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const running = useRef(false);

  const run = useCallback(async (action: () => Promise<void>): Promise<boolean> => {
    if (running.current) {
      return false;
    }
    running.current = true;
    setBusy(true);
    setError(null);
    try {
      await action();
      return true;
    } catch (failure) {
      if (showUnauthorized || !(failure instanceof ApiError && failure.status === 401)) {
        setError(messageOf(failure));
      }
      return false;
    } finally {
      running.current = false;
      setBusy(false);
    }
  }, [showUnauthorized]);

  return { busy, error, setError, run };
}

/** A refusal or failure, read out by screen readers as soon as it appears. */
export function ErrorMessage({ message }: { message: string | null }) {
  return message ? <p className="alert" role="alert">{message}</p> : null;
}

export function Loading() {
  return <p className="loading" role="status">Loading…</p>;
}

/** An empty list, with the exact text of the story (e.g. "No tasks yet."). */
export function Empty({ children }: { children: ReactNode }) {
  return <p className="empty">{children}</p>;
}

type FieldProps = { id: string; label: string; children: ReactNode };

/** A label above its control; the control must have the same `id`. */
export function Field({ id, label, children }: FieldProps) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children}
    </div>
  );
}

const STATUS_CLASS: Record<Status, string> = { TODO: "todo", IN_PROGRESS: "progress", DONE: "done" };

export const statusClass = (status: Status) => STATUS_CLASS[status];

export function StatusPill({ status }: { status: Status }) {
  return <span className={`pill pill-${STATUS_CLASS[status]}`}>{STATUS_LABEL[status]}</span>;
}

export function PriorityPill({ priority }: { priority: Priority }) {
  return <span className={`pill pill-${priority.toLowerCase()}`}>{PRIORITY_LABEL[priority]}</span>;
}

export function RolePill({ role }: { role: "MANAGER" | "MEMBER" }) {
  return <span className="pill pill-muted">{role === "MANAGER" ? "Manager" : "Member"}</span>;
}

/** Where the screen sits: "My projects / Website / Create logo". The last item is the current screen. */
export function Crumbs({ items }: { items: { label: string; href?: string }[] }) {
  return (
    <nav className="crumbs" aria-label="Breadcrumb">
      <ol>
        {items.map((item, index) => (
          <li key={index}>
            {item.href ? <a href={item.href}>{item.label}</a> : <span aria-current="page">{item.label}</span>}
          </li>
        ))}
      </ol>
    </nav>
  );
}

/** "2026-11-10 14:30" in the browser's local time (US21 AC01). */
export function dateTime(iso: string): string {
  const date = new Date(iso);
  const two = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${two(date.getMonth() + 1)}-${two(date.getDate())} ${two(date.getHours())}:${two(date.getMinutes())}`;
}

/**
 * Switches between the light theme (the default) and the dark one. The icon shows the theme it switches to (a moon
 * to go dark, a sun to go light); the button's accessible name and tooltip say it in words.
 */
export function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>(savedTheme);
  const next: Theme = theme === "light" ? "dark" : "light";
  const label = `Switch to ${next} mode`;
  return (
    <button type="button" className="btn btn-small btn-icon" aria-label={label} title={label}
      onClick={() => { applyTheme(next); setTheme(next); }}>
      {next === "dark" ? <MoonIcon /> : <SunIcon />}
    </button>
  );
}

function MoonIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d="M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z" fill="currentColor" />
    </svg>
  );
}

function SunIcon() {
  const rays = [0, 45, 90, 135, 180, 225, 270, 315];
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <circle cx="12" cy="12" r="4.5" fill="currentColor" />
      {rays.map((angle) => (
        <line key={angle} x1="12" y1="2.5" x2="12" y2="5" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
          transform={`rotate(${angle} 12 12)`} />
      ))}
    </svg>
  );
}
