import { useEffect, useState } from "react";
import { ApiError, getProject, Project, updateProject } from "./api";
import { ConfirmDelete } from "./ConfirmDelete";
import { ROLE_LABEL } from "./Dashboard";
import { ProjectForm } from "./ProjectForm";

type Props = { projectId: string; onLeave: () => void };

// A project (US04 AC02; wireframe screen 2 without the board, which arrives with US06): name, role and
// description, and Edit (US14) and Delete project (US15) for the manager only.
export function ProjectPage({ projectId, onLeave }: Props) {
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
          setError(failure instanceof Error ? failure.message : String(failure));   // e.g. "Project not found."
        }
      });
    return () => { current = false; };
  }, [projectId]);

  async function save(name: string, description: string) {
    setProject(await updateProject(projectId, name, description));
    setEditing(false);
  }

  return (
    <main>
      <p><a href="#">My projects</a></p>
      {error && <p role="alert">{error}</p>}
      {project && (editing ? (
        <ProjectForm submitLabel="Save" initialName={project.name} initialDescription={project.description ?? ""}
          onSubmit={save} onCancel={() => setEditing(false)} />
      ) : (
        <>
          <h1>{project.name}</h1>
          <p>{ROLE_LABEL[project.role]}</p>
          {project.description && <p>{project.description}</p>}
          {project.role === "MANAGER" && (
            <p>
              <button type="button" onClick={() => setEditing(true)}>Edit</button>{" "}
              <button type="button" onClick={() => setDeleting(true)}>Delete project</button>
            </p>
          )}
          {deleting && (
            <ConfirmDelete project={project} onDeleted={onLeave} onCancel={() => setDeleting(false)} />
          )}
        </>
      ))}
    </main>
  );
}
