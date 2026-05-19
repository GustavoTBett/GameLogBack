package com.gamelog.gamelog.controller.dto.steam;

import com.gamelog.gamelog.model.SteamAccount;

import java.time.Instant;

public record SteamAccountResponse(
        boolean linked,
        String steamId,
        String profileUrl,
        Instant lastSyncedAt,
        boolean synced
) {

    public static SteamAccountResponse from(SteamAccount account) {
        if (account == null) {
            return new SteamAccountResponse(false, null, null, null, false);
        }

        return new SteamAccountResponse(
                true,
                account.getSteamId(),
                account.getProfileUrl(),
                account.getLastSyncedAt(),
                Boolean.TRUE.equals(account.getSynced())
        );
    }
}
