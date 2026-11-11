-- Tasks module: new limits for TASK, decided by the user on 2026-11-11 (US05, docs/database.md):
--   title 3–150 characters (was 1–200), description up to 2000 (was 5000),
--   status value TODO instead of TO_DO (the interface still shows "To do").
-- V4_0 was already applied, so it is changed here and never edited (AGENTS.md §10).

-- Stop with a clear message, instead of a constraint error, if existing rows don't fit the new limits.
do $$
begin
    if exists (select 1 from task
               where char_length(title) not between 3 and 150
                  or char_length(description) > 2000) then
        raise exception 'Tasks with a title outside 3-150 characters or a description over 2000 characters exist; fix them before this migration';
    end if;
end
$$;

alter table task drop constraint task_status_check;
update task set status = 'TODO' where status = 'TO_DO';
alter table task alter column status set default 'TODO';
alter table task add constraint task_status_check
    check (status in ('TODO', 'IN_PROGRESS', 'DONE'));                                        -- R05

alter table task drop constraint task_title_check;
alter table task alter column title type varchar(150);
alter table task add constraint task_title_check check (char_length(title) between 3 and 150);

alter table task alter column description type varchar(2000);
