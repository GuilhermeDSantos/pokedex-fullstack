-- The User model the tasks belong to; only what authentication needs.
CREATE TABLE users (
    id            UUID         NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
);
