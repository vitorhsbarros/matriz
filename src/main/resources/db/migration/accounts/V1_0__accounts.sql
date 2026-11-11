-- Accounts module: USER (docs/database.md, section 3).
-- The table is called "users" because "user" is a reserved word in PostgreSQL.
create table users (
    id            uuid primary key,
    name          varchar(100) not null check (char_length(name) between 1 and 100),
    email         varchar(254) not null unique check (email = lower(email)),   -- R01, D03
    password_hash varchar(255) not null                                        -- NFR04: never the password
);
