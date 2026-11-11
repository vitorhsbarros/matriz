-- Tasks module: TASK, TASK_ASSIGNEE_USER, TASK_ASSIGNEE_TEAM and COMMENT (docs/database.md, section 3).
create table task (
    id          uuid primary key,
    project_id  uuid not null references project (id) on delete restrict,           -- R06; R17 via the event
    title       varchar(200) not null check (char_length(title) between 1 and 200),
    description varchar(5000),
    due_date    date,
    status      varchar(20) not null default 'TO_DO'
                check (status in ('TO_DO', 'IN_PROGRESS', 'DONE')),                  -- R05
    priority    varchar(10) not null default 'MEDIUM'
                check (priority in ('LOW', 'MEDIUM', 'HIGH')),                       -- R20
    created_at  timestamp with time zone not null default now()
);

create index task_project on task (project_id);

-- Assignees: zero or more people and teams per task, each at most once (R04).
-- They belong to their task: deleting the task deletes them (R22).
create table task_assignee_user (
    task_id uuid not null references task (id) on delete cascade,
    user_id uuid not null references users (id) on delete restrict,
    primary key (task_id, user_id)
);

create table task_assignee_team (
    task_id uuid not null references task (id) on delete cascade,
    team_id uuid not null references team (id) on delete restrict,                  -- R19 via the event
    primary key (task_id, team_id)
);

create index task_assignee_user_user on task_assignee_user (user_id);
create index task_assignee_team_team on task_assignee_team (team_id);

-- COMMENT is called "task_comment" because COMMENT is an SQL keyword.
create table task_comment (
    id         uuid primary key,
    task_id    uuid not null references task (id) on delete cascade,                 -- R21, R22
    author_id  uuid not null references users (id) on delete restrict,               -- R21
    text       varchar(2000) not null check (char_length(text) between 1 and 2000),
    created_at timestamp with time zone not null default now(),
    edited_at  timestamp with time zone                                               -- "(edited)" when set (US22)
);

create index task_comment_task on task_comment (task_id, created_at);
