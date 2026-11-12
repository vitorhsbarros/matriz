import { useEffect, useState } from "react";
import { currentSession, logOut, Session, whenLoggedOut } from "./api";
import { Dashboard } from "./Dashboard";
import { LogInPage } from "./LogInPage";
import { Logo } from "./Logo";
import { ProjectPage } from "./ProjectPage";
import { TaskPage } from "./TaskPage";
import { TeamsPage } from "./TeamsPage";
import { ErrorMessage, Loading, ThemeToggle, useAction } from "./ui";

const PLEASE_LOG_IN = "Please log in to continue.";

type State =
  | { screen: "loading" }
  | { screen: "logIn"; notice: string | null }
  | { screen: "app"; session: Session };

/** Where the user is, kept in the address after "#", so a reload stays there. */
export type Place =
  | { page: "home" }
  | { page: "teams" }
  | { page: "project"; projectId: string; tab: "board" | "members" }
  | { page: "task"; projectId: string; taskId: string };

export function placeInAddress(hash = window.location.hash): Place {
  const task = /^#\/projects\/([^/]+)\/tasks\/([^/]+)$/.exec(hash);
  if (task) {
    return { page: "task", projectId: decodeURIComponent(task[1]), taskId: decodeURIComponent(task[2]) };
  }
  const project = /^#\/projects\/([^/]+)(\/members)?$/.exec(hash);
  if (project) {
    return { page: "project", projectId: decodeURIComponent(project[1]), tab: project[2] ? "members" : "board" };
  }
  return hash === "#/teams" ? { page: "teams" } : { page: "home" };
}

function usePlace() {
  const [place, setPlace] = useState(() => placeInAddress());
  useEffect(() => {
    const update = () => setPlace(placeInAddress());
    window.addEventListener("hashchange", update);
    return () => window.removeEventListener("hashchange", update);
  }, []);
  return place;
}

export function go(address: string) {
  // Changes the address without a reload; "hashchange" then shows the screen.
  window.location.hash = address;
}

// The log-in screen (US02) and, once logged in, the page shell: the logo, the navigation (My projects, Teams),
// who is logged in and Log out (US02 AC10), around the current screen. All interface text is in English
// (C05, DoD01).
export function App() {
  const [state, setState] = useState<State>({ screen: "loading" });
  const place = usePlace();
  const logOutAction = useAction();

  useEffect(() => {
    // Whenever the API says the session ended (log-out elsewhere, expiry), go back to log-in.
    whenLoggedOut(() => setState({ screen: "logIn", notice: PLEASE_LOG_IN }));
    currentSession()
      .then((session) => setState(session ? { screen: "app", session } : { screen: "logIn", notice: PLEASE_LOG_IN }))
      .catch(() => setState({ screen: "logIn", notice: null }));
  }, []);

  async function handleLogOut() {
    if (await logOutAction.run(logOut)) {
      go("");
      setState({ screen: "logIn", notice: null });
    }
  }

  if (state.screen === "loading") {
    return <main className="page"><Loading /></main>;
  }
  if (state.screen === "logIn") {
    return <LogInPage notice={state.notice} onLoggedIn={(session) => setState({ screen: "app", session })} />;
  }

  const me = state.session.user;
  return (
    <>
      <header className="shell-header">
        <div className="shell-bar">
          <Logo href="#" />
          <nav className="shell-nav" aria-label="Main">
            <a href="#" aria-current={place.page === "home" ? "page" : undefined}>My projects</a>
            <a href="#/teams" aria-current={place.page === "teams" ? "page" : undefined}>Teams</a>
          </nav>
          <div className="shell-user">
            <ThemeToggle />
            <span>{me.name}</span>
            <button type="button" className="btn btn-small" onClick={handleLogOut} disabled={logOutAction.busy}>
              Log out
            </button>
          </div>
        </div>
      </header>
      <main className="page">
        <ErrorMessage message={logOutAction.error} />
        {place.page === "task" ? (
          <TaskPage key={place.taskId} projectId={place.projectId} taskId={place.taskId} me={me}
            onLeave={() => go(`#/projects/${place.projectId}`)} />
        ) : place.page === "project" ? (
          <ProjectPage key={place.projectId} projectId={place.projectId} tab={place.tab} onLeave={() => go("")} />
        ) : place.page === "teams" ? (
          <TeamsPage me={me} />
        ) : (
          <Dashboard />
        )}
      </main>
    </>
  );
}
