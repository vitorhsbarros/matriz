import { FormEvent, useCallback, useEffect, useState } from "react";
import { addMember, ApiError, listMembers, Member, removeMember } from "./api";
import { ErrorMessage, Field, Loading, messageOf, RolePill, useAction } from "./ui";

type Props = { projectId: string; manager: boolean };

// A project's members (US09): every member sees names, emails and roles (PRI03). The manager adds people by email
// and removes members, but not themself (D07). Messages come from the API word for word (SEC09: "This user could
// not be added." whatever the reason). After a change the list is read again from the API.
export function MembersPage({ projectId, manager }: Props) {
  const [members, setMembers] = useState<Member[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [email, setEmail] = useState("");
  const adding = useAction();
  const removing = useAction();

  const load = useCallback(async () => {
    try {
      setMembers(await listMembers(projectId));
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

  function add(event: FormEvent) {
    event.preventDefault();
    adding.run(async () => {
      await addMember(projectId, email);
      setEmail("");          // cleared only once the API confirmed it
      await load();
    });
  }

  return (
    <section className="panel" aria-label="Members">
      <h2>Members</h2>
      <ErrorMessage message={error} />
      {manager && (
        <form className="inline-form" onSubmit={add} noValidate aria-label="Add a member">
          <Field id="member-email" label="Email">
            <input id="member-email" className="input" type="email" value={email}
              onChange={(event) => setEmail(event.target.value)} />
          </Field>
          <button type="submit" className="btn btn-primary" disabled={adding.busy}>Add member</button>
        </form>
      )}
      <ErrorMessage message={adding.error ?? removing.error} />
      {!members && !error && <Loading />}
      {members && (
        <ul className="list" aria-label="Project members">
          {members.map((member) => (
            <li key={member.userId}>
              <span>
                <strong>{member.name}</strong> <span className="pill-muted">{member.email}</span>
              </span>
              <span className="actions">
                <RolePill role={member.role} />
                {manager && member.role !== "MANAGER" && (
                  <button type="button" className="btn btn-quiet btn-small" aria-label={`Remove ${member.name}`}
                    disabled={removing.busy}
                    onClick={() => removing.run(async () => { await removeMember(projectId, member.userId); await load(); })}>
                    Remove
                  </button>
                )}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
