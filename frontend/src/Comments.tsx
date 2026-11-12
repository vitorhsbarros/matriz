import { FormEvent, useCallback, useEffect, useState } from "react";
import { addComment, ApiError, Comment, deleteComment, editComment, listComments, User } from "./api";
import { ConfirmDelete, DELETE_COMMENT_MESSAGE } from "./ConfirmDelete";
import { dateTime, Empty, ErrorMessage, Field, Loading, messageOf, useAction } from "./ui";

type Props = { projectId: string; taskId: string; me: User };

// A task's comments (US21, US22): oldest first, each with its author and date and time, and "(edited)" once
// edited. Any member comments; only the author sees Edit and Delete on a comment (SEC11), and the API refuses
// anyone else anyway. After every change the list is read again from the API.
export function Comments({ projectId, taskId, me }: Props) {
  const [comments, setComments] = useState<Comment[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [text, setText] = useState("");
  const [editing, setEditing] = useState<{ id: string; text: string } | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);
  const adding = useAction();
  const saving = useAction();

  const load = useCallback(async () => {
    try {
      setComments(await listComments(projectId, taskId));
      setError(null);
    } catch (failure) {
      if (!(failure instanceof ApiError && failure.status === 401)) {
        setError(messageOf(failure));
      }
    }
  }, [projectId, taskId]);

  useEffect(() => {
    load();
  }, [load]);

  function add(event: FormEvent) {
    event.preventDefault();
    adding.run(async () => {
      await addComment(projectId, taskId, text);
      setText("");   // cleared only once the API confirmed it
      await load();
    });
  }

  function save(event: FormEvent) {
    event.preventDefault();
    if (!editing) {
      return;
    }
    saving.run(async () => {
      await editComment(projectId, taskId, editing.id, editing.text);
      setEditing(null);
      await load();
    });
  }

  return (
    <section className="comments" aria-label="Comments">
      <h2 className="section-title">Comments</h2>
      <ErrorMessage message={error} />
      {!comments && !error && <Loading />}
      {comments && comments.length === 0 && <Empty>No comments yet.</Empty>}
      {comments?.map((comment) => {
        const mine = comment.author.userId === me.id;
        return (
          <article key={comment.id} className="comment" aria-label={`Comment by ${comment.author.name}`}>
            <div className="comment-head">
              <span>
                <strong>{comment.author.name}</strong> · {dateTime(comment.createdAt)}
                {comment.editedAt && " (edited)"}
              </span>
              {mine && editing?.id !== comment.id && (
                <span className="actions">
                  <button type="button" className="btn btn-quiet btn-small"
                    onClick={() => { saving.setError(null); setEditing({ id: comment.id, text: comment.text }); }}>Edit</button>
                  <button type="button" className="btn btn-quiet btn-small" onClick={() => setDeleting(comment.id)}>Delete</button>
                </span>
              )}
            </div>
            {editing?.id === comment.id ? (
              <form className="form" onSubmit={save} noValidate aria-label="Edit comment">
                <ErrorMessage message={saving.error} />
                <Field id="edit-comment" label="Comment">
                  <textarea id="edit-comment" className="input" value={editing.text}
                    onChange={(event) => setEditing({ ...editing, text: event.target.value })} />
                </Field>
                <div className="actions">
                  <button type="submit" className="btn btn-primary btn-small" disabled={saving.busy}>Save</button>
                  <button type="button" className="btn btn-small" onClick={() => setEditing(null)}>Cancel</button>
                </div>
              </form>
            ) : (
              <p className="prose">{comment.text}</p>
            )}
            {deleting === comment.id && (
              <ConfirmDelete message={DELETE_COMMENT_MESSAGE} confirmLabel="Delete comment"
                onConfirm={() => deleteComment(projectId, taskId, comment.id)}
                onDeleted={() => { setDeleting(null); load(); }}
                onCancel={() => setDeleting(null)} />
            )}
          </article>
        );
      })}
      <form className="form" onSubmit={add} noValidate aria-label="New comment">
        <ErrorMessage message={adding.error} />
        <Field id="new-comment" label="Add a comment">
          <textarea id="new-comment" className="input" value={text} onChange={(event) => setText(event.target.value)} />
        </Field>
        <div className="actions">
          <button type="submit" className="btn btn-primary" disabled={adding.busy}>Comment</button>
        </div>
      </form>
    </section>
  );
}
