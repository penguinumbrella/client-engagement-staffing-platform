CREATE TABLE login_attempts (

    id UUID PRIMARY KEY,

    user_id UUID,

    email VARCHAR(254) NOT NULL,

    successful BOOLEAN NOT NULL,

    failure_reason VARCHAR(100),

    attempted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_login_attempts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_login_attempts_user_id
    ON login_attempts (user_id);

CREATE INDEX idx_login_attempts_email
    ON login_attempts (email);

CREATE INDEX idx_login_attempts_attempted_at
    ON login_attempts (attempted_at);

CREATE INDEX idx_login_attempts_successful
    ON login_attempts (successful);