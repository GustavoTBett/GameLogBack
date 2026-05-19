package com.gamelog.gamelog.controller.dto;

import java.time.Instant;

public record GameReviewResponse(
        Long id,
        Integer score,
        String review,
        String username,
        Instant createdAt,
        Instant updatedAt,
        Long upvoteCount,
        Long downvoteCount,
        String userVote,
        String source,
        Boolean recommended,
        Boolean canEdit,
        Boolean canVote
) {
}
