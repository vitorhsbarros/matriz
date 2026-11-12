import { FormEvent, useState } from "react";
import { logIn, Session } from "./api";
import { Logo } from "./Logo";
import { ErrorMessage, Field, ThemeToggle, useAction } from "./ui";

type Props = {
  /** Why the log-in screen is shown, e.g. "Please log in to continue." (DoD02). */
  notice: string | null;
  onLoggedIn: (session: Session) => void;
};

// US02: log in with email and password. The messages come from the API, word for word (DoD04): "Invalid email or
// password.", "Email and password are required.". The fields aren't marked "required", so the browser doesn't
// replace the API's message with its own. The typed email stays after a failure.
export function LogInPage({ notice, onLoggedIn }: Props) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const action = useAction({ showUnauthorized: true });

  function submit(event: FormEvent) {
    event.preventDefault();
    action.run(async () => onLoggedIn(await logIn(email, password)));
  }

  return (
    <main className="login">
      <div className="login-theme"><ThemeToggle /></div>
      <Logo large />
      <p className="tagline">Every project. Every task. Everyone in the picture.</p>
      <div className="status-rule" aria-hidden="true" />
      <form className="panel form" onSubmit={submit} noValidate aria-label="Log in">
        <h1>Log in</h1>
        {notice && !action.error && <p className="notice" role="alert">{notice}</p>}
        <ErrorMessage message={action.error} />
        <Field id="email" label="Email">
          <input id="email" className="input" type="email" autoComplete="username" value={email}
            onChange={(event) => setEmail(event.target.value)} />
        </Field>
        <Field id="password" label="Password">
          <input id="password" className="input" type="password" autoComplete="current-password" value={password}
            onChange={(event) => setPassword(event.target.value)} />
        </Field>
        <div className="actions">
          <button type="submit" className="btn btn-primary" disabled={action.busy}>Log in</button>
        </div>
      </form>
    </main>
  );
}
