CREATE TABLE tasks (
    id          UUID          NOT NULL,
    owner_id    UUID          NOT NULL,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    status      VARCHAR(20)   NOT NULL,
    due_date    DATE,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_tasks PRIMARY KEY (id),
    CONSTRAINT fk_tasks_users FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'))
);

-- Every read is "this owner's tasks", optionally by status.
CREATE INDEX ix_tasks_owner_id_status ON tasks (owner_id, status);
