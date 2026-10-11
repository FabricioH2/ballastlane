-- Optimistic locking: every update must match the version it was read at.
ALTER TABLE tasks ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
