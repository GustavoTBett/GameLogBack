package com.gamelog.gamelog.service.ratingvote;

import com.gamelog.gamelog.exception.EntityCannotBeNull;
import com.gamelog.gamelog.model.enums.RatingVoteType;
import com.gamelog.gamelog.model.Rating;
import com.gamelog.gamelog.model.RatingVote;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.RatingVoteRepository;
import com.gamelog.gamelog.service.rating.RatingService;
import com.gamelog.gamelog.service.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class RatingVoteServiceImpl implements RatingVoteService {

    private final RatingService ratingService;
    private final UserService userService;
    private final RatingVoteRepository ratingVoteRepository;

    public RatingVoteServiceImpl(
            RatingService ratingService,
            UserService userService,
            RatingVoteRepository ratingVoteRepository
    ) {
        this.ratingService = ratingService;
        this.userService = userService;
        this.ratingVoteRepository = ratingVoteRepository;
    }

    @Override
    @Transactional
    public void vote(Long ratingId, Long userId, RatingVoteType voteType) {
        Rating rating = ratingService.get(ratingId)
                .orElseThrow(() -> new EntityCannotBeNull("Rating not found with id " + ratingId));
        User user = userService.get(userId)
                .orElseThrow(() -> new EntityCannotBeNull("User not found with id " + userId));

        if (rating.getUser() != null && Objects.equals(rating.getUser().getId(), user.getId())) {
            throw new IllegalArgumentException("You cannot vote your own review");
        }

        RatingVote existingVote = ratingVoteRepository.findByRatingIdAndUserId(ratingId, userId).orElse(null);
        if (existingVote == null) {
            ratingVoteRepository.save(RatingVote.builder()
                    .rating(rating)
                    .user(user)
                    .voteType(voteType)
                    .build());
            return;
        }

        if (existingVote.getVoteType() == voteType) {
            ratingVoteRepository.delete(existingVote);
            return;
        }

        existingVote.setVoteType(voteType);
        ratingVoteRepository.save(existingVote);
    }

    @Override
    @Transactional
    public void removeVote(Long ratingId, Long userId) {
        ratingVoteRepository.deleteByRatingIdAndUserId(ratingId, userId);
    }

    @Override
    public Map<Long, RatingVoteStats> getVoteStatsByRatingIds(Collection<Long> ratingIds) {
        if (ratingIds == null || ratingIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, RatingVoteStats> statsByRatingId = new HashMap<>();
        ratingVoteRepository.countVotesByRatingIds(ratingIds).forEach(tuple -> {
            Long ratingId = (Long) tuple[0];
            long upvoteCount = tuple[1] != null ? ((Number) tuple[1]).longValue() : 0L;
            long downvoteCount = tuple[2] != null ? ((Number) tuple[2]).longValue() : 0L;
            statsByRatingId.put(ratingId, new RatingVoteStats(upvoteCount, downvoteCount));
        });
        return statsByRatingId;
    }

    @Override
    public Map<Long, RatingVoteType> getUserVotesByRatingIds(Collection<Long> ratingIds, Long userId) {
        if (ratingIds == null || ratingIds.isEmpty() || userId == null) {
            return Map.of();
        }

        Map<Long, RatingVoteType> votesByRatingId = new HashMap<>();
        ratingVoteRepository.findVoteTypesByRatingIdsAndUserId(ratingIds, userId).forEach(tuple -> {
            Long ratingId = (Long) tuple[0];
            RatingVoteType voteType = (RatingVoteType) tuple[1];
            votesByRatingId.put(ratingId, voteType);
        });
        return votesByRatingId;
    }
}