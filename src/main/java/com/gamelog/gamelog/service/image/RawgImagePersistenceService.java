package com.gamelog.gamelog.service.image;

import com.gamelog.gamelog.model.EnumUser.ImageSource;
import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.repository.GameRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class RawgImagePersistenceService {

    private final GameRepository gameRepository;

    public RawgImagePersistenceService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistRawgImageIfChanged(Long gameId, String rawgImage, OffsetDateTime now) {
        Game managedGame = gameRepository.findById(gameId).orElse(null);
        if (managedGame == null) {
            return;
        }

        boolean changed = !rawgImage.equals(managedGame.getRawgImageUrl()) || managedGame.getImageSource() != ImageSource.RAWG;
        if (!changed) {
            return;
        }

        managedGame.setRawgImageUrl(rawgImage);
        managedGame.setImageSource(ImageSource.RAWG);
        managedGame.setImageLastCheckedAt(now);

        try {
            gameRepository.save(managedGame);
        } catch (ObjectOptimisticLockingFailureException ignored) {
            // Ignore race between concurrent reads updating same game image.
        }
    }
}
