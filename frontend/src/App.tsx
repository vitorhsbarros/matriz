import { useEffect, useState } from "react";
import { ApiError, currentSession, logOut, Session, whenLoggedOut } from "./api";
import { LogInPage } from "./LogInPage";

const PLEASE_LOG_IN = "Please log in to continue.";

type State =
  | { screen: "loading" }
  | { screen: "logIn"; notice: string | null }
  | { screen: "home"; session: Session; error: string | null };

// Increment I1 (US02): the log-in screen, and a header with who is logged in and "Log out".
// The home dashboard (US04) replaces the home screen's content in I2.
// All interface text is in English (C05, DoD01).
export function App() {
  const [state, setState] = useState<State>({ screen: "loading" });

  useEffect(() => {
    // Whenever the API says the session ended (log-out elsewhere, expiry), go back to log-in.
    whenLoggedOut(() => setState({ screen: "logIn", notice: PLEASE_LOG_IN }));
    currentSession()
      .then((session) =>
        setState(session
          ? { screen: "home", session, error: null }
          : { screen: "logIn", notice: PLEASE_LOG_IN }))
      .catch(() => setState({ screen: "logIn", notice: null }));
  }, []);

  async function handleLogOut(session: Session) {
    try {
      await logOut();
      setState({ screen: "logIn", notice: null });
    } catch (error) {
      if (!(error instanceof ApiError && error.status === 401)) {
        setState({ screen: "home", session, error: error instanceof Error ? error.message : String(error) });
      }
    }
  }

  switch (state.screen) {
    case "loading":
      return null;
    case "logIn":
      return (
        <LogInPage notice={state.notice}
          onLoggedIn={(session) => setState({ screen: "home", session, error: null })} />
      );
    case "home":
      return (
        <>
          <header>
            <strong>Matriz</strong> <span>{state.session.user.name}</span>{" "}
            <button type="button" onClick={() => handleLogOut(state.session)}>Log out</button>
          </header>
          <main>
            {state.error && <p role="alert">{state.error}</p>}
            <h1>Home</h1>
          </main>
        </>
      );
  }
}
