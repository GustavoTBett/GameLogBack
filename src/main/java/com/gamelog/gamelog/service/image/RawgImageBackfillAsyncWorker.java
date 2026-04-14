package com.gamelog.gamelog.service.image;

import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
public class RawgImageBackfillAsyncWorker {

    private static final Logger log = LoggerFactory.getLogger(RawgImageBackfillAsyncWorker.class);

    private final GameRepository gameRepository;
    private final RawgGameImageResolver rawgGameImageResolver;

    public RawgImageBackfillAsyncWorker(GameRepository gameRepository, RawgGameImageResolver rawgGameImageResolver) {
        this.gameRepository = gameRepository;
        this.rawgGameImageResolver = rawgGameImageResolver;
    }

    @Async
    public CompletableFuture<Set<Long>> processGameIdsAsync(Set<Long> gameIds) {
        log.info("RAWG async backfill started ids={}", gameIds.size());

        int attempted = 0;
        for (Game game : gameRepository.findAllById(gameIds)) {
            if (!StringUtils.hasText(game.getRawgImageUrl())) {
                rawgGameImageResolver.enrichRawgImageIfMissing(game);
                attempted++;
            }
        }

        log.info("RAWG async backfill finished ids={} attempted={}", gameIds.size(), attempted);
        return CompletableFuture.completedFuture(new HashSet<>(gameIds));
    }
}
