ALTER TABLE app_user
    ADD COLUMN role TEXT NOT NULL DEFAULT 'USER';

ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_role CHECK (role IN ('USER', 'ADMIN'));

UPDATE app_user
SET email = lower(trim(email)),
    username = trim(username)
WHERE email <> lower(trim(email)) OR username <> trim(username);
