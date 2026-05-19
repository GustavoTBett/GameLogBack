package com.gamelog.gamelog.service.steam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class SteamServiceTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private SteamService steamService;

    @BeforeEach
    void setup() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        steamService = new SteamService(restTemplate, "DUMMY_KEY");
    }

    @Test
    void getPlayerSummary_parsesResponse() {
        String body = "{\"response\":{\"players\":[{\"steamid\":\"12345\",\"personaname\":\"gamer\"}]}}";
        server.expect(requestTo(org.hamcrest.Matchers.containsString("GetPlayerSummaries")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        Map<String, Object> player = steamService.getPlayerSummary("12345");
        assertThat(player.get("personaname")).isEqualTo("gamer");
    }

    @Test
    void resolveVanityUrl_parsesSteamId() {
        String body = "{\"response\":{\"success\":1,\"steamid\":\"76561198000000000\"}}";
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ResolveVanityURL")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String steamId = steamService.resolveVanityUrl("myvanity");
        assertThat(steamId).isEqualTo("76561198000000000");
    }

    @Test
    void resolveVanityUrl_whenNotFound_returnsNull() {
        String body = "{\"response\":{\"success\":42}}";
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ResolveVanityURL")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String steamId = steamService.resolveVanityUrl("missing");
        assertThat(steamId).isNull();
    }

    @Test
    void getUserReviews_parsesPositiveAndNegativeRecommendations() {
        String listing = """
                <a href="https://steamcommunity.com/profiles/76561198000000000/recommended/111/">Liked</a>
                <a href="https://steamcommunity.com/profiles/76561198000000000/recommended/222/">Disliked</a>
                """;
        String emptyListing = "<html></html>";
        String positiveReview = """
                <div class="apphub_AppName">Liked Game</div>
                <div id="ReviewText">Loved it</div>
                <div class="title"><span>Recommended</span></div>
                <div class="date_posted">Posted: Jan 10, 2026 @ 1:30PM</div>
                """;
        String negativeReview = """
                <div class="apphub_AppName">Disliked Game</div>
                <div id="ReviewText">Did not like it</div>
                <div class="title"><span>Not</span><span>Recommended</span></div>
                <div class="date_posted">Posted: Jan 11, 2026 @ 2:30PM</div>
                """;

        server.expect(requestTo(org.hamcrest.Matchers.containsString("/recommended/?p=1")))
                .andRespond(withSuccess(listing, MediaType.TEXT_HTML));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/recommended/?p=2")))
                .andRespond(withSuccess(emptyListing, MediaType.TEXT_HTML));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/recommended/111/")))
                .andRespond(withSuccess(positiveReview, MediaType.TEXT_HTML));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/recommended/222/")))
                .andRespond(withSuccess(negativeReview, MediaType.TEXT_HTML));

        List<Map<String, Object>> reviews = steamService.getUserReviews("76561198000000000");

        Map<String, Object> liked = reviews.stream()
                .filter(review -> Long.valueOf(111L).equals(review.get("appid")))
                .findFirst()
                .orElseThrow();
        Map<String, Object> disliked = reviews.stream()
                .filter(review -> Long.valueOf(222L).equals(review.get("appid")))
                .findFirst()
                .orElseThrow();

        assertThat(liked.get("voted_up")).isEqualTo(true);
        assertThat(disliked.get("voted_up")).isEqualTo(false);
    }

    @Test
    void getUserReviews_parsesRecommendationFromTextBeforeReviewBlock() {
        String listing = """
                <a href="https://steamcommunity.com/profiles/76561198000000000/recommended/333/">Liked</a>
                """;
        String emptyListing = "<html></html>";
        String reviewPage = """
                <html>
                  <body>
                    <div class="profile_small_header_texture">
                      Recommended
                      21.0 hrs on record
                    </div>
                    <div id="ReviewText">Um otimo jogo, alem de nao ser muito caro</div>
                  </body>
                </html>
                """;

        server.expect(requestTo(org.hamcrest.Matchers.containsString("/recommended/?p=1")))
                .andRespond(withSuccess(listing, MediaType.TEXT_HTML));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/recommended/?p=2")))
                .andRespond(withSuccess(emptyListing, MediaType.TEXT_HTML));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/recommended/333/")))
                .andRespond(withSuccess(reviewPage, MediaType.TEXT_HTML));

        List<Map<String, Object>> reviews = steamService.getUserReviews("76561198000000000");

        assertThat(reviews).hasSize(1);
        assertThat(reviews.getFirst().get("voted_up")).isEqualTo(true);
    }
}
