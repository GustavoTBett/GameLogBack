package com.gamelog.gamelog.repository;

import com.gamelog.gamelog.model.enums.RatingVoteType;
import com.gamelog.gamelog.model.RatingVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RatingVoteRepository extends JpaRepository<RatingVote, Long> {

    Optional<RatingVote> findByRatingIdAndUserId(Long ratingId, Long userId);

    void deleteByRatingIdAndUserId(Long ratingId, Long userId);

    @Query("""
            SELECT rv.rating.id,
                   SUM(CASE WHEN rv.voteType = com.gamelog.gamelog.model.enums.RatingVoteType.UPVOTE THEN 1 ELSE 0 END),
                   SUM(CASE WHEN rv.voteType = com.gamelog.gamelog.model.enums.RatingVoteType.DOWNVOTE THEN 1 ELSE 0 END)
            FROM RatingVote rv
            WHERE rv.rating.id IN :ratingIds
            GROUP BY rv.rating.id
            """)
    List<Object[]> countVotesByRatingIds(@Param("ratingIds") Collection<Long> ratingIds);

    @Query("""
            SELECT rv.rating.id, rv.voteType
            FROM RatingVote rv
            WHERE rv.rating.id IN :ratingIds AND rv.user.id = :userId
            """)
    List<Object[]> findVoteTypesByRatingIdsAndUserId(@Param("ratingIds") Collection<Long> ratingIds, @Param("userId") Long userId);
}