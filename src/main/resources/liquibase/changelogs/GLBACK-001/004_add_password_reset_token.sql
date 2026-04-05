CREATE TABLE password_reset_token (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    token_hash TEXT NOT NULL UNIQUE,

    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,

    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_password_reset_token_user
        FOREIGN KEY (user_id)
            REFERENCES app_user(id)
            ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_token_user_id
    ON password_reset_token(user_id);

CREATE INDEX idx_password_reset_token_expires_at
    ON password_reset_token(expires_at);
