package com.gamelog.gamelog.service.image;

import com.gamelog.gamelog.model.Game;

public interface GameImageResolver {

    String resolveAndPersistCoverUrl(Game game);

    void enrichRawgMetadataIfMissing(Game game);
}
