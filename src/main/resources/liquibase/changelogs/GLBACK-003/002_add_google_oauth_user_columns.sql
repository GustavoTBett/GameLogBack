ALTER TABLE app_user
    ADD COLUMN google_sub TEXT;

ALTER TABLE app_user
    ADD CONSTRAINT uk_app_user_google_sub UNIQUE (google_sub);

ALTER TABLE app_user
    ADD COLUMN google_email_verified BOOLEAN NOT NULL DEFAULT FALSE;