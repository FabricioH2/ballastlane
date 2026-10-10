-- Users who can log in to the API. Only the BCrypt hash of the password is stored.
CREATE TABLE users (
    id            BIGSERIAL                PRIMARY KEY,
    username      VARCHAR(50)              NOT NULL,
    password_hash VARCHAR(100)             NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
);
