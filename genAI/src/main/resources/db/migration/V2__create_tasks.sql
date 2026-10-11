CREATE TABLE tasks (
    id          UUID          PRIMARY KEY,
    owner_id    UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(120)  NOT NULL,
    description VARCHAR(2000),
    status      VARCHAR(20)   NOT NULL CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    due_date    DATE,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_tasks_owner_created ON tasks (owner_id, created_at DESC);
CREATE INDEX idx_tasks_owner_status_created ON tasks (owner_id, status, created_at DESC);
