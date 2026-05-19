package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.controller.dto.steam.SteamAccountResponse;
import com.gamelog.gamelog.controller.dto.steam.SteamSyncSummaryResponse;
import com.gamelog.gamelog.controller.dto.steam.SteamUserReviewResponse;
import com.gamelog.gamelog.model.SteamAccount;
import com.gamelog.gamelog.repository.SteamAccountRepository;
import com.gamelog.gamelog.repository.SteamUserReviewRepository;
import com.gamelog.gamelog.service.steam.SteamSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/steam")
public class SteamController {

    private final SteamAccountRepository steamAccountRepository;
    private final SteamUserReviewRepository steamUserReviewRepository;
    private final SteamSyncService steamSyncService;

    public SteamController(
            SteamAccountRepository steamAccountRepository,
            SteamUserReviewRepository steamUserReviewRepository,
            SteamSyncService steamSyncService
    ) {
        this.steamAccountRepository = steamAccountRepository;
        this.steamUserReviewRepository = steamUserReviewRepository;
        this.steamSyncService = steamSyncService;
    }

    @GetMapping("/me")
    public ResponseEntity<SteamAccountResponse> getMyAccount(Authentication authentication) {
        return ResponseEntity.ok(SteamAccountResponse.from(resolveAccount(authentication).orElse(null)));
    }

    @Transactional(readOnly = true)
    @GetMapping("/me/reviews")
    public ResponseEntity<List<SteamUserReviewResponse>> getMyReviews(Authentication authentication) {
        SteamAccount account = resolveAccount(authentication).orElse(null);
        if (account == null) {
            return ResponseEntity.ok(List.of());
        }

        List<SteamUserReviewResponse> reviews = steamUserReviewRepository.findAllBySteamAccount(account)
                .stream()
            .filter(review -> !Boolean.FALSE.equals(review.getActive()))
                .sorted(Comparator.comparing(
                        review -> review.getReviewedAt() != null ? review.getReviewedAt() : review.getImportedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(SteamUserReviewResponse::from)
                .toList();

        return ResponseEntity.ok(reviews);
    }

    @PostMapping("/me/sync")
    public ResponseEntity<SteamSyncSummaryResponse> syncMyAccount(Authentication authentication) {
        SteamAccount account = resolveAccount(authentication).orElse(null);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        SteamSyncService.ImportSummary summary = steamSyncService.importUserReviews(
                account,
                account.getProfileUrl() != null ? account.getProfileUrl() : account.getSteamId()
        );
        return ResponseEntity.ok(SteamSyncSummaryResponse.from(summary));
    }

    private Optional<SteamAccount> resolveAccount(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            return Optional.empty();
        }

        return steamAccountRepository.findByUserId(principal.getId());
    }
}
