package com.gamelog.gamelog.service.recommendation;

import com.gamelog.gamelog.model.Game;

public interface RawgGameImportService {

    /**
     * Busca um jogo na RAWG API por nome e o importa no banco se encontrado.
     * Se não existir, lança exceção.
     *
     * @param gameName Nome do jogo a buscar
     * @return Game importado e salvo
     * @throws RuntimeException se jogo não encontrado na RAWG
     */
    Game importGameByName(String gameName);

    /**
     * Busca um jogo na RAWG API por slug e o importa ou atualiza no banco.
     * O slug é o formato canônico usado pela RAWG para localizar o jogo.
     *
     * @param slug Slug do jogo na RAWG
     * @return Game importado e salvo
     * @throws RuntimeException se jogo não encontrado na RAWG
     */
    Game importGameBySlug(String slug);

    /**
     * Busca um jogo por ID do RAWG e o importa (ou atualiza) no banco.
     *
     * @param rawgId ID do jogo na RAWG
     * @return Game importado
     */
    Game importGameByRawgId(Long rawgId);
}
