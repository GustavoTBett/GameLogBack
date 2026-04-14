package com.gamelog.gamelog.service.image;

import com.gamelog.gamelog.model.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RawgImageBackfillService {

    private static final Logger log = LoggerFactory.getLogger(RawgImageBackfillService.class);

    private final RawgImageBackfillAsyncWorker rawgImageBackfillAsyncWorker;
    private final Set<Long> inFlightGameIds = ConcurrentHashMap.newKeySet();

    public RawgImageBackfillService(RawgImageBackfillAsyncWorker rawgImageBackfillAsyncWorker) {
        this.rawgImageBackfillAsyncWorker = rawgImageBackfillAsyncWorker;
    }

    public void triggerBackfillForGames(Collection<Game> games) {
        Set<Long> idsToProcess = games.stream()
                .filter(game -> game.getId() != null)
                .filter(game -> !StringUtils.hasText(game.getRawgImageUrl()))
                .map(Game::getId)
                .filter(inFlightGameIds::add)
                .collect(Collectors.toSet());

        if (idsToProcess.isEmpty()) {
            log.debug("RAWG async backfill skipped (all requested games already have RAWG image or are in-flight)");
            return;
        }

        log.info("RAWG async backfill triggered ids={}", idsToProcess.size());
        rawgImageBackfillAsyncWorker.processGameIdsAsync(idsToProcess)
                .whenComplete((processedIds, throwable) -> {
                    inFlightGameIds.removeAll(idsToProcess);
                    if (throwable != null) {
                        log.warn("RAWG async backfill failed ids={} reason={}", idsToProcess.size(), throwable.getMessage());
                    }
                });
    }
}
