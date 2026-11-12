import { useCallback, useEffect, useState } from "react";
import {
  addAssignee, ApiError, assigneeLabel, EligibleAssignees, listEligibleAssignees, listTeams, removeAssignee, Task, Team,
} from "./api";
import { ErrorMessage, Field, Loading, messageOf, useAction } from "./ui";

type Props = { projectId: string; task: Task; onChange: (task: Task) => void };

// A task's assignees (US10, US13): any member adds or removes people and teams (D08). The choices come from
// listEligibleAssignees (the project's members, and teams whose members are all in it, D09); those already
// assigned are left out. Teams that can't be assigned because not all their members are in the project are shown
// disabled, with the rule's own message (US13 AC04), so a team never just goes missing. The API checks again when
// assigning (US10 AC05), and the task it returns replaces the one on screen, so what is shown is what was saved.
export function Assignees({ projectId, task, onChange }: Props) {
  const [eligible, setEligible] = useState<EligibleAssignees | null>(null);
  const [allTeams, setAllTeams] = useState<Team[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [choice, setChoice] = useState("");
  const action = useAction();

  const load = useCallback(async () => {
    try {
      const [choices, teams] = await Promise.all([listEligibleAssignees(projectId), listTeams()]);
      setEligible(choices);
      setAllTeams(teams);
      setError(null);
    } catch (failure) {
      if (!(failure instanceof ApiError && failure.status === 401)) {
        setError(messageOf(failure));
      }
    }
  }, [projectId]);

  useEffect(() => {
    load();
  }, [load]);

  const assignedPeople = new Set(task.assignees.flatMap((a) => (a.type === "person" ? [a.userId] : [])));
  const assignedTeams = new Set(task.assignees.flatMap((a) => (a.type === "team" ? [a.teamId] : [])));
  const people = eligible?.people.filter((person) => !assignedPeople.has(person.userId)) ?? [];
  const teams = eligible?.teams.filter((team) => !assignedTeams.has(team.teamId)) ?? [];
  const eligibleTeamIds = new Set(eligible?.teams.map((team) => team.teamId) ?? []);
  const outsideTeams = allTeams.filter((team) => !eligibleTeamIds.has(team.id) && !assignedTeams.has(team.id));

  function assign() {
    if (!choice) {
      return;
    }
    const [type, id] = choice.split(":");
    action.run(async () => {
      onChange(await addAssignee(projectId, task.id, type === "team" ? { type: "team", teamId: id } : { type: "person", userId: id }));
      setChoice("");
      await load();   // a team may have stopped being eligible in the meantime
    });
  }

  return (
    <section className="panel" aria-label="Assignees">
      <h2 className="section-title">Assignees</h2>
      {task.assignees.length === 0 ? (
        <p>Unassigned</p>
      ) : (
        <ul className="list" aria-label="Task assignees">
          {task.assignees.map((assignee) => (
            <li key={assignee.type === "person" ? assignee.userId : assignee.teamId}>
              <span>{assigneeLabel(assignee)}</span>
              <button type="button" className="btn btn-quiet btn-small" aria-label={`Remove ${assigneeLabel(assignee)}`}
                disabled={action.busy}
                onClick={() => action.run(async () => onChange(await removeAssignee(projectId, task.id, assignee)))}>
                Remove
              </button>
            </li>
          ))}
        </ul>
      )}
      <ErrorMessage message={error ?? action.error} />
      {!eligible && !error && <Loading />}
      {eligible && (people.length > 0 || teams.length > 0 || outsideTeams.length > 0) && (
        <div className="inline-form">
          <Field id="assignee" label="Add an assignee">
            <select id="assignee" className="input" value={choice} onChange={(event) => setChoice(event.target.value)}>
              <option value="">Choose a person or a team</option>
              {people.length > 0 && (
                <optgroup label="People">
                  {people.map((person) => <option key={person.userId} value={`person:${person.userId}`}>{person.name}</option>)}
                </optgroup>
              )}
              {(teams.length > 0 || outsideTeams.length > 0) && (
                <optgroup label="Teams">
                  {teams.map((team) => <option key={team.teamId} value={`team:${team.teamId}`}>{team.name} (team)</option>)}
                  {outsideTeams.map((team) => (
                    <option key={team.id} value={`team:${team.id}`} disabled>
                      {team.name} (team) (not all members are in the project)
                    </option>
                  ))}
                </optgroup>
              )}
            </select>
          </Field>
          <button type="button" className="btn btn-primary" onClick={assign} disabled={action.busy || !choice}>Assign</button>
        </div>
      )}
      {outsideTeams.length > 0 && <p className="pill-muted">All team members must belong to the project.</p>}
    </section>
  );
}
