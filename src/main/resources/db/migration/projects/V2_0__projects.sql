-- Projects module: PROJECT and PROJECT_MEMBER (docs/database.md, section 3).
-- References to other modules use foreign keys with ON DELETE RESTRICT (PD04): the database
-- refuses a delete that would leave a dangling reference, so a failed event handler is caught.
create table project (
    id          uuid primary key,
    name        varchar(100) not null check (char_length(name) between 1 and 100),  -- may repeat (D06)
    description varchar(2000),
    manager_id  uuid not null references users (id) on delete restrict               -- R07, D07
);

-- Members belong to their project: deleting the project deletes them (R17).
create table project_member (
    project_id uuid not null references project (id) on delete cascade,
    user_id    uuid not null references users (id) on delete restrict,
    primary key (project_id, user_id)                                                 -- R02
);

create index project_member_user on project_member (user_id);
