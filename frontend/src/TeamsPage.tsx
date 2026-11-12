import { FormEvent, useCallback, useEffect, useState } from "react";
import {
  addTeamMember, ApiError, createTeam, deleteTeam, listTeams, removeTeamMember, renameTeam, Team, User,
} from "./api";
import { ConfirmDelete, deleteTeamMessage } from "./ConfirmDelete";
import { Empty, ErrorMessage, Field, Loading, messageOf, RolePill, useAction } from "./ui";

type Props = { me: User };

// Teams (US11, US12, US17, US18; the wireframe has no screen for them, so they reuse its patterns). Every user sees
// every team with its members' names (D05, PD11) and can create one, becoming its manager (D07). Only the team's
// manager sees Rename, Delete team and the member controls; the API refuses anyone else anyway. After every change
// the list is read again from the API.
export function TeamsPage({ me }: Props) {
  const [teams, setTeams] = useState<Team[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState("");
  const creation = useAction();

  const load = useCallback(async () => {
    try {
      setTeams(await listTeams());
      setError(null);
    } catch (failure) {
      if (!(failure instanceof ApiError && failure.status === 401)) {
        setError(messageOf(failure));
      }
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  function create(event: FormEvent) {
    event.preventDefault();
    creation.run(async () => {
      await createTeam(name);
      setName("");
      setCreating(false);
      await load();
    });
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Teams</h1>
          <p className="lede">Assign a whole team to a task, as long as all its members are in the project.</p>
        </div>
        {!creating && <button type="button" className="btn btn-primary" onClick={() => setCreating(true)}>New team</button>}
      </div>

      {creating && (
        <form className="panel form" onSubmit={create} noValidate aria-label="New team">
          <h2>New team</h2>
          <ErrorMessage message={creation.error} />
          <Field id="team-name" label="Name">
            <input id="team-name" className="input" value={name} onChange={(event) => setName(event.target.value)} />
          </Field>
          <div className="actions">
            <button type="submit" className="btn btn-primary" disabled={creation.busy}>Create team</button>
            <button type="button" className="btn" onClick={() => { setCreating(false); creation.setError(null); }}>Cancel</button>
          </div>
        </form>
      )}

      <ErrorMessage message={error} />
      {!teams && !error && <Loading />}
      {teams && teams.length === 0 && <Empty>No teams yet.</Empty>}
      {teams && teams.length > 0 && (
        <div className="team-grid">
          {teams.map((team) => <TeamCard key={team.id} team={team} me={me} onChanged={load} />)}
        </div>
      )}
    </>
  );
}

function TeamCard({ team, me, onChanged }: { team: Team; me: User; onChanged: () => Promise<void> }) {
  const manager = team.members.some((member) => member.userId === me.id && member.role === "MANAGER");
  const [renaming, setRenaming] = useState(false);
  const [newName, setNewName] = useState(team.name);
  const [deleting, setDeleting] = useState(false);
  const [email, setEmail] = useState("");
  const renameAction = useAction();
  const memberAction = useAction();

  function rename(event: FormEvent) {
    event.preventDefault();
    renameAction.run(async () => {
      await renameTeam(team.id, newName);
      setRenaming(false);
      await onChanged();
    });
  }

  function addMember(event: FormEvent) {
    event.preventDefault();
    memberAction.run(async () => {
      await addTeamMember(team.id, email);
      setEmail("");
      await onChanged();
    });
  }

  return (
    <section className="panel" aria-label={`Team ${team.name}`}>
      {renaming ? (
        <form className="form" onSubmit={rename} noValidate aria-label="Rename team">
          <ErrorMessage message={renameAction.error} />
          <Field id={`rename-${team.id}`} label="Name">
            <input id={`rename-${team.id}`} className="input" value={newName} onChange={(event) => setNewName(event.target.value)} />
          </Field>
          <div className="actions">
            <button type="submit" className="btn btn-primary btn-small" disabled={renameAction.busy}>Save</button>
            <button type="button" className="btn btn-small" onClick={() => { setRenaming(false); setNewName(team.name); }}>Cancel</button>
          </div>
        </form>
      ) : (
        <div className="page-head">
          <h2>{team.name}</h2>
          {manager && (
            <div className="actions">
              <button type="button" className="btn btn-small" onClick={() => { renameAction.setError(null); setRenaming(true); }}>Rename</button>
              <button type="button" className="btn btn-danger btn-small" onClick={() => setDeleting(true)}>Delete team</button>
            </div>
          )}
        </div>
      )}
      {deleting && (
        <ConfirmDelete message={deleteTeamMessage(team.name)} confirmLabel="Delete team"
          onConfirm={() => deleteTeam(team.id)}
          onDeleted={() => { setDeleting(false); onChanged(); }}
          onCancel={() => setDeleting(false)} />
      )}
      <ul className="list" aria-label={`Members of ${team.name}`}>
        {team.members.map((member) => (
          <li key={member.userId}>
            <span>{member.name}</span>
            <span className="actions">
              <RolePill role={member.role} />
              {manager && member.role !== "MANAGER" && (
                <button type="button" className="btn btn-quiet btn-small" aria-label={`Remove ${member.name} from ${team.name}`}
                  disabled={memberAction.busy}
                  onClick={() => memberAction.run(async () => { await removeTeamMember(team.id, member.userId); await onChanged(); })}>
                  Remove
                </button>
              )}
            </span>
          </li>
        ))}
      </ul>
      {manager && (
        <form className="inline-form" onSubmit={addMember} noValidate aria-label={`Add a member to ${team.name}`}>
          <Field id={`team-email-${team.id}`} label="Email">
            <input id={`team-email-${team.id}`} className="input" type="email" value={email}
              onChange={(event) => setEmail(event.target.value)} />
          </Field>
          <button type="submit" className="btn btn-small" disabled={memberAction.busy}>Add member</button>
        </form>
      )}
      <ErrorMessage message={memberAction.error} />
    </section>
  );
}
