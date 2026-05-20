-- Allow multiple Gamelog users to link the same Steam profile.
DROP INDEX IF EXISTS ux_steam_account_steam_id;

CREATE INDEX IF NOT EXISTS idx_steam_account_steam_id ON steam_account(steam_id);
