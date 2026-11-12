import { useEffect, useState } from "react";
import { ApiError, deleteProject, getProject, Project, updateProject } from "./api";
import { Board } from "./Board";
import { ConfirmDelete, deleteProjectMessage } from "./ConfirmDelete";
import { MembersPage } from "./MembersPage";
import { ProjectForm } from "./ProjectForm";
import { Crumbs, ErrorMessage, Loading, messageOf, RolePill } from "./ui";

type Props = { projectId: string; tab: "board" | "members"; onLeave: () => void };

// A project (US04 AC02; wireframe screen 2): name, role and description, Edit (US14) and Delete project (US15)
// for the manager only, and two tabs: the task board (US06) and the members (US09).
export function ProjectPage({ projectId, tab, onLeave }: Props) {
  const [project, setProject] = useState<Project | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    let current = true;
    setProject(null);
    setError(null);
    getProject(projectId)
      .then((result) => current && setProject(result))
      .catch((failure) => {
        if (current && !(failure instanceof ApiError && failure.status === 401)) {
          setError(messageOf(failure));   // e.g. "Project not found."
        }
      });
    return () => { current = false; };
  }, [projectId]);

  async function save(name: string, description: string) {
    setProject(await updateProject(projectId, name, description));
    setEditing(false);
  }

  const base = `#/projects/${projectId}`;
  return (
    <>
      <Crumbs items={[{ label: "My projects", href: "#" }, { label: project?.name ?? "Project" }]} />
      <ErrorMessage message={error} />
      {!project && !error && <Loading />}
      {project && (editing ? (
        <ProjectForm title="Edit project" submitLabel="Save" initialName={project.name}
          initialDescription={project.description ?? ""} onSubmit={save} onCancel={() => setEditing(false)} />
      ) : (
        <>
          <div className="page-head">
            <div>
              <div className="actions">
                <h1>{project.name}</h1>
                <RolePill role={project.role} />
              </div>
              {project.description && <p className="lede">{project.description}</p>}
            </div>
            {project.role === "MANAGER" && (
              <div className="actions">
                <button type="button" className="btn" onClick={() => setEditing(true)}>Edit</button>
                <button type="button" className="btn btn-danger" onClick={() => setDeleting(true)}>Delete project</button>
              </div>
            )}
          </div>
          {deleting && (
            <ConfirmDelete message={deleteProjectMessage(project.name)} confirmLabel="Delete project"
              onConfirm={() => deleteProject(projectId)} onDeleted={onLeave} onCancel={() => setDeleting(false)} />
          )}
          <nav className="tabs" aria-label="Project">
            <a href={base} aria-current={tab === "board" ? "page" : undefined}>Board</a>
            <a href={`${base}/members`} aria-current={tab === "members" ? "page" : undefined}>Members</a>
          </nav>
          {tab === "board"
            ? <Board projectId={projectId} />
            : <MembersPage projectId={projectId} manager={project.role === "MANAGER"} />}
        </>
      ))}
    </>
  );
}
