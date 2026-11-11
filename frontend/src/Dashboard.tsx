import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError, createProject, getDashboard, ProjectDashboard, ProjectSummary, TaskCounts } from "./api";
import { ConfirmDelete } from "./ConfirmDelete";
import { ProjectForm } from "./ProjectForm";

export const ROLE_LABEL = { MANAGER: "Manager", MEMBER: "Member" } as const;

function Counts({ counts }: { counts: TaskCounts }) {
  return (
    <span>
      To do {counts.toDo} · In progress {counts.inProgress} · Done {counts.done}
    </span>
  );
}

// Home dashboard (US04, US23; wireframe screen 1): the user's projects with their role and task counts, the
// totals, the search, New project (US03) and Delete for the projects the user manages (US15).
export function Dashboard() {
  const [search, setSearch] = useState("");
  const [dashboard, setDashboard] = useState<ProjectDashboard | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<ProjectSummary | null>(null);
  const latest = useRef(0);

  const load = useCallback(async (text: string) => {
    const request = ++latest.current;
    try {
      const result = await getDashboard(text);
      if (request === latest.current) {
        setDashboard(result);
        setError(null);
      }
    } catch (failure) {
      if (request === latest.current && !(failure instanceof ApiError && failure.status === 401)) {
        setError(failure instanceof Error ? failure.message : String(failure));
      }
    }
  }, []);

  useEffect(() => {
    load(search);
  }, [load, search]);

  async function create(name: string, description: string) {
    await createProject(name, description);
    setCreating(false);
    await load(search);
  }

  const searching = search.trim() !== "";
  const empty = dashboard !== null && dashboard.projects.length === 0;

  return (
    <main>
      <h1>My projects</h1>
      {error && <p role="alert">{error}</p>}

      {creating ? (
        <ProjectForm submitLabel="Create project" onSubmit={create} onCancel={() => setCreating(false)} />
      ) : (
        <button type="button" onClick={() => setCreating(true)}>New project</button>
      )}

      <p>
        <label htmlFor="search">Search projects</label>{" "}
        <input id="search" type="search" value={search} onChange={(event) => setSearch(event.target.value)} />
      </p>

      {deleting && (
        <ConfirmDelete project={deleting}
          onDeleted={() => { setDeleting(null); load(search); }}
          onCancel={() => setDeleting(null)} />
      )}

      {dashboard && empty && !searching && <p>You are not a member of any project yet.</p>}
      {dashboard && !(empty && !searching) && (
        <>
          <p><strong>All my projects</strong> <Counts counts={dashboard.totals} /></p>
          {empty ? (
            <p>No projects match your search.</p>
          ) : (
            <ul aria-label="Projects">
              {dashboard.projects.map((project) => (
                <li key={project.id}>
                  <a href={`#/projects/${project.id}`}>{project.name}</a>{" "}
                  <span>{ROLE_LABEL[project.role]}</span>{" "}
                  <Counts counts={project.taskCounts} />{" "}
                  {project.role === "MANAGER" && (
                    <button type="button" aria-label={`Delete ${project.name}`}
                      onClick={() => setDeleting(project)}>Delete</button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </main>
  );
}
