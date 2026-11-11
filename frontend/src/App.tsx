import { useEffect, useState } from "react";
import { ApiError, currentSession, logOut, Session, whenLoggedOut } from "./api";
import { Dashboard } from "./Dashboard";
import { LogInPage } from "./LogInPage";
import { ProjectPage } from "./ProjectPage";

const PLEASE_LOG_IN = "Please log in to continue.";

type State =
  | { screen: "loading" }
  | { screen: "logIn"; notice: string | null }
  | { screen: "app"; session: Session; error: string | null };

/** The screen is kept in the address (#/projects/<id>), so a reload stays where the user was. */
function projectInAddress(): string | null {
  const match = /^#\/projects\/([^/]+)$/.exec(window.location.hash);
  return match ? decodeURIComponent(match[1]) : null;
}

function useProjectInAddress() {
  const [projectId, setProjectId] = useState(projectInAddress);
  useEffect(() => {
    const update = () => setProjectId(projectInAddress());
    window.addEventListener("hashchange", update);
    return () => window.removeEventListener("hashchange", update);
  }, []);
  return projectId;
}

function goToDashboard() {
  // Clears the address without a reload; hashchange then shows the dashboard.
  window.location.hash = "";
}

// The log-in screen (US02), and once logged in: the header with who is logged in and "Log out", the home
// dashboard (US04) and a project (US04 AC02). All interface text is in English (C05, DoD01).
export function App() {
  const [state, setState] = useState<State>({ screen: "loading" });
  const projectId = useProjectInAddress();

  useEffect(() => {
    // Whenever the API says the session ended (log-out elsewhere, expiry), go back to log-in.
    whenLoggedOut(() => setState({ screen: "logIn", notice: PLEASE_LOG_IN }));
    currentSession()
      .then((session) =>
        setState(session
          ? { screen: "app", session, error: null }
          : { screen: "logIn", notice: PLEASE_LOG_IN }))
      .catch(() => setState({ screen: "logIn", notice: null }));
  }, []);

  async function handleLogOut(session: Session) {
    try {
      await logOut();
      goToDashboard();
      setState({ screen: "logIn", notice: null });
    } catch (error) {
      if (!(error instanceof ApiError && error.status === 401)) {
        setState({ screen: "app", session, error: error instanceof Error ? error.message : String(error) });
      }
    }
  }

  switch (state.screen) {
    case "loading":
      return null;
    case "logIn":
      return (
        <LogInPage notice={state.notice}
          onLoggedIn={(session) => setState({ screen: "app", session, error: null })} />
      );
    case "app":
      return (
        <>
          <header>
            <strong>Matriz</strong> <span>{state.session.user.name}</span>{" "}
            <button type="button" onClick={() => handleLogOut(state.session)}>Log out</button>
          </header>
          {state.error && <p role="alert">{state.error}</p>}
          {projectId ? <ProjectPage projectId={projectId} onLeave={goToDashboard} /> : <Dashboard />}
        </>
      );
  }
}
