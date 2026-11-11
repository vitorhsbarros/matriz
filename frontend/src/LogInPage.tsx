import { FormEvent, useState } from "react";
import { ApiError, logIn, Session } from "./api";

type Props = {
  /** Why the log-in screen is shown, e.g. "Please log in to continue." (DoD02). */
  notice: string | null;
  onLoggedIn: (session: Session) => void;
};

// US02: log in with email and password. The messages come from the API, word for word (DoD04).
// The fields aren't marked "required", so the browser doesn't replace the API's message with its own.
export function LogInPage({ notice, onLoggedIn }: Props) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSending(true);
    setError(null);
    try {
      onLoggedIn(await logIn(email, password));
    } catch (failure) {
      setError(failure instanceof ApiError ? failure.message : String(failure));
      setSending(false);
    }
  }

  const message = error ?? notice;
  return (
    <main>
      <h1>Matriz</h1>
      <h2>Log in</h2>
      {message && <p role="alert">{message}</p>}
      <form onSubmit={submit} noValidate>
        <p>
          <label htmlFor="email">Email</label>
          <br />
          <input id="email" type="email" autoComplete="username" value={email}
            onChange={(event) => setEmail(event.target.value)} />
        </p>
        <p>
          <label htmlFor="password">Password</label>
          <br />
          <input id="password" type="password" autoComplete="current-password" value={password}
            onChange={(event) => setPassword(event.target.value)} />
        </p>
        <button type="submit" disabled={sending}>Log in</button>
      </form>
    </main>
  );
}
