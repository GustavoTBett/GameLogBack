package com.gamelog.gamelog.service.image;

import com.gamelog.gamelog.model.EnumUser.ImageSource;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

@Service
public class RawgImagePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(RawgImagePersistenceService.class);

    private final GameRepository gameRepository;

    public RawgImagePersistenceService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistRawgImageIfChanged(Long gameId, String rawgImage, OffsetDateTime now) {
        persistRawgMetadataIfChanged(gameId, rawgImage, null, null, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistRawgMetadataIfChanged(
            Long gameId,
            String rawgImage,
            String rawgDescription,
            String rawgDescriptionPtBr,
            OffsetDateTime now
    ) {
        Game managedGame = gameRepository.findById(gameId).orElse(null);
        if (managedGame == null) {
            log.warn("RAWG metadata persistence skipped gameId={} (game not found)", gameId);
            return;
        }

        boolean hasNewImage = StringUtils.hasText(rawgImage);
        boolean hasNewDescription = StringUtils.hasText(rawgDescription);
        boolean hasNewDescriptionPtBr = StringUtils.hasText(rawgDescriptionPtBr);

        boolean imageChanged = hasNewImage
                && (!rawgImage.equals(managedGame.getRawgImageUrl()) || managedGame.getImageSource() != ImageSource.RAWG);
        boolean descriptionChanged = hasNewDescription && !rawgDescription.equals(managedGame.getDescription());
        boolean descriptionPtBrChanged = hasNewDescriptionPtBr && !rawgDescriptionPtBr.equals(managedGame.getDescriptionPtBr());

        log.debug(
            "RAWG metadata persistence decision gameId={} hasNewImage={} hasNewDescription={} hasNewDescriptionPtBr={} imageChanged={} descriptionChanged={} descriptionPtBrChanged={}",
            gameId,
            hasNewImage,
            hasNewDescription,
            hasNewDescriptionPtBr,
            imageChanged,
            descriptionChanged,
            descriptionPtBrChanged
        );

        if (!imageChanged && !descriptionChanged && !descriptionPtBrChanged) {
            log.debug("RAWG metadata persistence skipped gameId={} (no changes)", gameId);
            return;
        }

        if (imageChanged) {
            managedGame.setRawgImageUrl(rawgImage);
            managedGame.setImageSource(ImageSource.RAWG);
        }

        if (descriptionChanged) {
            managedGame.setDescription(rawgDescription);
        }

        if (descriptionPtBrChanged) {
            managedGame.setDescriptionPtBr(rawgDescriptionPtBr);
        }

        managedGame.setImageLastCheckedAt(now);

        try {
            gameRepository.save(managedGame);
            log.info(
                    "RAWG metadata persisted gameId={} persistedImage={} persistedDescription={} persistedDescriptionPtBr={}",
                    gameId,
                    imageChanged,
                    descriptionChanged,
                    descriptionPtBrChanged
            );
        } catch (ObjectOptimisticLockingFailureException ignored) {
            // Ignore race between concurrent reads updating same game image.
            log.warn("RAWG metadata persistence race ignored gameId={}", gameId);
        }
    }
}
