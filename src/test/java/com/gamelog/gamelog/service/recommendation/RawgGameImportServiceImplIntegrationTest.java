package com.gamelog.gamelog.service.recommendation;

import com.gamelog.gamelog.model.Game;
import com.gamelog.gamelog.model.GamePlatformMapping;
import com.gamelog.gamelog.model.Genre;
import com.gamelog.gamelog.model.enums.GamePlatform;
import com.gamelog.gamelog.repository.GameGenreRepository;
import com.gamelog.gamelog.repository.GamePlatformMappingRepository;
import com.gamelog.gamelog.repository.GameRepository;
import com.gamelog.gamelog.repository.GenreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

public class RawgGameImportServiceImplIntegrationTest {

    private HttpServer server;
    private int port;
    private final AtomicInteger translationCalls = new AtomicInteger();
    private final AtomicInteger genreIdSequence = new AtomicInteger(1000);
    private String rawgSearchResponse;
    private String rawgDetailResponse;
    private int translationStatus = 200;
    private String translationResponse = "{\"translatedText\":\"texto traduzido\"}";

    private GameRepository gameRepository;
    private GamePlatformMappingRepository gamePlatformMappingRepository;
    private GameGenreRepository gameGenreRepository;
    private GenreRepository genreRepository;
    private RawgGameImportServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        gameRepository = mock(GameRepository.class);
        gamePlatformMappingRepository = mock(GamePlatformMappingRepository.class);
        gameGenreRepository = mock(GameGenreRepository.class);
        genreRepository = mock(GenreRepository.class);

        when(gameRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        when(gamePlatformMappingRepository.findAllByGameIdIn(anyCollection())).thenReturn(List.of());
        when(gameGenreRepository.findAllByGameIdsWithGenre(anyCollection())).thenReturn(List.of());
        when(genreRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(genreRepository.save(any(Genre.class))).thenAnswer(invocation -> {
            Genre genre = invocation.getArgument(0);
            ReflectionTestUtils.setField(genre, "id", (long) genreIdSequence.incrementAndGet());
            return genre;
        });
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game game = invocation.getArgument(0);
            ReflectionTestUtils.setField(game, "id", 1L);
            return game;
        });

        service = new RawgGameImportServiceImpl(
                gameRepository,
                gamePlatformMappingRepository,
                gameGenreRepository,
                genreRepository,
                new ObjectMapper()
        );

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/rawg", new RawgHandler());
        server.createContext("/translate", new TranslateHandler());
        server.start();
        port = server.getAddress().getPort();

        ReflectionTestUtils.setField(service, "rawgApiKey", "test-key");
        ReflectionTestUtils.setField(service, "rawgBaseUrl", "http://localhost:" + port + "/rawg");
        ReflectionTestUtils.setField(service, "rawgTimeoutMs", 2000);
        ReflectionTestUtils.setField(service, "translationBaseUrl", "http://localhost:" + port);
        ReflectionTestUtils.setField(service, "translationTimeoutMs", 2000);
        ReflectionTestUtils.setField(service, "translationConnectTimeoutMs", 2000);
        ReflectionTestUtils.setField(service, "translationRetryCount", 0);
        ReflectionTestUtils.setField(service, "translationSourceLang", "en");
        ReflectionTestUtils.setField(service, "translationTargetLang", "pt");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void importGameByName_cadastraCamposMinimos_ePersisteIdsExternos() {
        rawgSearchResponse = """
                {"results":[{"slug":"elden-ring"}]}
                """;
        rawgDetailResponse = """
                {
                  "id": 1234,
                  "slug": "elden-ring",
                  "name": "Elden Ring",
                  "description_raw": "An epic action RPG.",
                  "released": "2022-02-25",
                  "background_image": "http://images.example/elden-ring.jpg",
                  "rating": 4.7,
                  "steam_appid": 1245620,
                  "developers": [{"name": "FromSoftware"}],
                  "publishers": [{"name": "Bandai Namco"}],
                  "parent_platforms": [
                    {"platform": {"slug": "pc", "name": "PC"}},
                    {"platform": {"slug": "playstation4", "name": "PlayStation 4"}}
                  ],
                  "genres": [
                    {"name": "Action"},
                    {"name": "RPG"}
                  ]
                }
                """;

        Game imported = service.importGameByName("Elden Ring");

        assertThat(imported.getName()).isEqualTo("Elden Ring");
        assertThat(imported.getSlug()).isEqualTo("elden-ring");
        assertThat(imported.getRawgId()).isEqualTo(1234L);
        assertThat(imported.getSteamAppId()).isEqualTo(1245620L);
        assertThat(imported.getDescription()).isEqualTo("An epic action RPG.");
        assertThat(imported.getDescriptionPtBr()).isEqualTo("texto traduzido");
        assertThat(imported.getReleaseDate()).isNotNull();
        assertThat(imported.getRawgImageUrl()).isEqualTo("http://images.example/elden-ring.jpg");
        assertThat(imported.getCover_url()).isEqualTo("http://images.example/elden-ring.jpg");
        assertThat(imported.getDeveloper()).isEqualTo("FromSoftware");
        assertThat(imported.getPublisher()).isEqualTo("Bandai Namco");

        assertThat(translationCalls.get()).isEqualTo(1);
        verify(gamePlatformMappingRepository, times(1)).saveAll(argThat(mappings -> sizeOf(mappings) == 2));
        verify(gameGenreRepository, times(1)).saveAll(argThat(mappings -> sizeOf(mappings) == 2));
    }

    @Test
    void importGameByName_naoTraduzQuandoTextoJaEstaEmPortugues() {
        rawgSearchResponse = """
                {"results":[{"slug":"jogo-em-portugues"}]}
                """;
        rawgDetailResponse = """
                {
                  "id": 2222,
                  "slug": "jogo-em-portugues",
                  "name": "Jogo em Português",
                  "description_raw": "Uma aventura épica e emocionante.",
                  "released": "2023-01-10",
                  "background_image": "http://images.example/pt.jpg",
                  "rating": 4.2,
                  "steam_appid": 5555,
                  "developers": [{"name": "Estúdio Local"}],
                  "publishers": [{"name": "Editora Local"}],
                  "genres": [{"name": "Ação"}]
                }
                """;

        Game imported = service.importGameByName("Jogo em Português");

        assertThat(imported.getDescriptionPtBr()).isEqualTo("Uma aventura épica e emocionante.");
        assertThat(translationCalls.get()).isZero();
    }

    @Test
    void importGameByName_naoReinserePlataformaJaExistenteParaOMesmoJogo() {
        rawgSearchResponse = """
                {"results":[{"slug":"elden-ring"}]}
                """;
        rawgDetailResponse = """
                {
                  "id": 1234,
                  "slug": "elden-ring",
                  "name": "Elden Ring",
                  "description_raw": "An epic action RPG.",
                  "released": "2022-02-25",
                  "background_image": "http://images.example/elden-ring.jpg",
                  "rating": 4.7,
                  "steam_appid": 1245620,
                  "parent_platforms": [
                    {"platform": {"slug": "pc", "name": "PC"}},
                    {"platform": {"slug": "playstation4", "name": "PlayStation 4"}}
                  ],
                  "genres": [{"name": "Action"}]
                }
                """;

        Game existingGame = new Game();
        ReflectionTestUtils.setField(existingGame, "id", 1L);
        GamePlatformMapping existingPlatform = GamePlatformMapping.builder()
                .game(existingGame)
                .platform(GamePlatform.PC)
                .build();
        when(gamePlatformMappingRepository.findAllByGameIdIn(anyCollection())).thenReturn(List.of(existingPlatform));

        service.importGameByName("Elden Ring");

        verify(gamePlatformMappingRepository, never()).deleteAll(anyCollection());
        verify(gamePlatformMappingRepository, times(1)).saveAll(argThat(mappings ->
                sizeOf(mappings) == 1 && containsOnlyPlatform(mappings, GamePlatform.PLAYSTATION)
        ));
    }

    @Test
    void importGameByRawgId_falhaNaTraducaoAindaImportaOJogo() {
        rawgDetailResponse = """
                {
                  "id": 9876,
                  "slug": "translation-fail-game",
                  "name": "Translation Fail Game",
                  "description_raw": "A mystery with failed translation.",
                  "released": "2021-12-01",
                  "background_image": "http://images.example/fail.jpg",
                  "rating": 4.0,
                  "steam_appid": 7777,
                  "developers": [{"name": "Fail Studio"}],
                  "publishers": [{"name": "Fail Publisher"}],
                  "genres": [{"name": "Adventure"}]
                }
                """;
        translationStatus = 500;
        translationResponse = "{}";

        Game imported = service.importGameByRawgId(9876L);

        assertThat(imported.getRawgId()).isEqualTo(9876L);
        assertThat(imported.getSteamAppId()).isEqualTo(7777L);
        assertThat(imported.getDescription()).isEqualTo("A mystery with failed translation.");
        assertThat(imported.getDescriptionPtBr()).isEqualTo("A mystery with failed translation.");
        assertThat(translationCalls.get()).isEqualTo(1);
        verify(gameRepository, times(1)).save(any(Game.class));
    }

    private class RawgHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();

            byte[] body;
            int status = 200;

            if ("/rawg/games".equals(path) && query != null && query.contains("search=")) {
                body = rawgSearchResponse.getBytes(StandardCharsets.UTF_8);
            } else if (path.startsWith("/rawg/games/")) {
                body = rawgDetailResponse.getBytes(StandardCharsets.UTF_8);
            } else {
                status = 404;
                body = "{}".getBytes(StandardCharsets.UTF_8);
            }

            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private class TranslateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            translationCalls.incrementAndGet();

            byte[] body = translationResponse.getBytes(StandardCharsets.UTF_8);
            int status = translationStatus;

            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private static int sizeOf(Iterable<?> values) {
        int count = 0;
        for (Object ignored : values) {
            count++;
        }

        return count;
    }

    private static boolean containsOnlyPlatform(Iterable<GamePlatformMapping> mappings, GamePlatform expectedPlatform) {
        for (GamePlatformMapping mapping : mappings) {
            if (mapping == null || mapping.getPlatform() != expectedPlatform) {
                return false;
            }
        }

        return true;
    }
}
