import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError, createProject, deleteProject, getDashboard, ProjectDashboard, ProjectSummary, TaskCounts } from "./api";
import { ConfirmDelete, deleteProjectMessage } from "./ConfirmDelete";
import { ProjectForm } from "./ProjectForm";
import { Empty, ErrorMessage, Loading, messageOf, RolePill } from "./ui";

export const ROLE_LABEL = { MANAGER: "Manager", MEMBER: "Member" } as const;

function Counts({ counts }: { counts: TaskCounts }) {
  return (
    <span className="totals">
      <span className="pill-todo count-dot">To do <span className="count">{counts.toDo}</span></span>{" "}
      <span className="pill-progress count-dot">In progress <span className="count">{counts.inProgress}</span></span>{" "}
      <span className="pill-done count-dot">Done <span className="count">{counts.done}</span></span>
    </span>
  );
}

// Home dashboard (US04, US23; wireframe screen 1): the user's projects with their role and task counts, the totals
// over every project whatever the search (D14), the search, New project (US03) and Delete for the projects the
// user manages (US15). After a change the list is read again from the API, so names and counts are the server's.
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
        setError(messageOf(failure));
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
    <>
      <div className="page-head">
        <div>
          <h1>My projects</h1>
          <p className="lede">Everything you're working on, and where it stands.</p>
        </div>
        {!creating && (
          <button type="button" className="btn btn-primary" onClick={() => setCreating(true)}>New project</button>
        )}
      </div>

      {creating && (
        <ProjectForm title="New project" submitLabel="Create project" onSubmit={create}
          onCancel={() => setCreating(false)} />
      )}

      <ErrorMessage message={error} />

      {deleting && (
        <ConfirmDelete message={deleteProjectMessage(deleting.name)} confirmLabel="Delete project"
          onConfirm={() => deleteProject(deleting.id)}
          onDeleted={() => { setDeleting(null); load(search); }}
          onCancel={() => setDeleting(null)} />
      )}

      {!dashboard && !error && <Loading />}
      {dashboard && empty && !searching && <Empty>You are not a member of any project yet.</Empty>}
      {dashboard && !(empty && !searching) && (
        <>
          <div className="panel">
            <p className="totals"><strong>All my projects</strong> <Counts counts={dashboard.totals} /></p>
          </div>
          <div className="inline-form">
            <div className="field">
              <label htmlFor="search">Search projects</label>
              <input id="search" className="input" type="search" placeholder="Project name" value={search}
                onChange={(event) => setSearch(event.target.value)} />
            </div>
          </div>
          {empty ? (
            <Empty>No projects match your search.</Empty>
          ) : (
            <div className="table-wrap">
              <table className="table" aria-label="Projects">
                <thead>
                  <tr>
                    <th scope="col">Project</th>
                    <th scope="col">My role</th>
                    <th scope="col" className="num">To do</th>
                    <th scope="col" className="num">In progress</th>
                    <th scope="col" className="num">Done</th>
                    <th scope="col"><span className="visually-hidden">Actions</span></th>
                  </tr>
                </thead>
                <tbody>
                  {dashboard.projects.map((project) => (
                    <tr key={project.id}>
                      <td className="name"><a href={`#/projects/${project.id}`}>{project.name}</a></td>
                      <td><RolePill role={project.role} /></td>
                      <td className="num">{project.taskCounts.toDo}</td>
                      <td className="num">{project.taskCounts.inProgress}</td>
                      <td className="num">{project.taskCounts.done}</td>
                      <td>
                        {project.role === "MANAGER" && (
                          <button type="button" className="btn btn-quiet btn-small" aria-label={`Delete ${project.name}`}
                            onClick={() => setDeleting(project)}>Delete</button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </>
  );
}
