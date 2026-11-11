-- Teams module: TEAM and TEAM_MEMBER (docs/database.md, section 3).
create table team (
    id         uuid primary key,
    name       varchar(50) not null check (char_length(name) between 1 and 50),
    manager_id uuid not null references users (id) on delete restrict                -- R07, D07
);

-- Team names are unique ignoring case (R03, D06); the name is stored as typed.
create unique index team_name_unique on team (lower(name));

create table team_member (
    team_id uuid not null references team (id) on delete cascade,
    user_id uuid not null references users (id) on delete restrict,
    primary key (team_id, user_id)                                                    -- R02
);
