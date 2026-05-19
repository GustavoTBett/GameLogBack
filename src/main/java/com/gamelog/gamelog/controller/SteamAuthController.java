package com.gamelog.gamelog.controller;

import com.gamelog.gamelog.config.security.AppUserPrincipal;
import com.gamelog.gamelog.model.SteamAccount;
import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.repository.SteamAccountRepository;
import com.gamelog.gamelog.service.user.UserService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class SteamAuthController {

    private final SteamAccountRepository steamAccountRepository;
    private final UserService userService;
    private final RestTemplate restTemplate;
    private final String frontendBaseUrl;

    public SteamAuthController(
            SteamAccountRepository steamAccountRepository,
            UserService userService,
            RestTemplate restTemplate,
            @Value("${app.frontend.base-url:http://localhost:3000}") String frontendBaseUrl
    ) {
        this.steamAccountRepository = steamAccountRepository;
        this.userService = userService;
        this.restTemplate = restTemplate;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @GetMapping("/steam")
    public ResponseEntity<Void> redirectToSteam(HttpServletRequest request) {
        String base = request.getRequestURL().toString().replace(request.getRequestURI(), "");
        String returnTo = base + "/auth/steam/callback";

        URI steamLoginUri = UriComponentsBuilder
            .fromUriString("https://steamcommunity.com/openid/login")
            .queryParam("openid.ns", "http://specs.openid.net/auth/2.0")
            .queryParam("openid.mode", "checkid_setup")
            .queryParam("openid.return_to", returnTo)
            .queryParam("openid.realm", base + "/")
            .queryParam("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select")
            .queryParam("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select")
            .build(true)
            .toUri();

        return ResponseEntity.status(302).location(steamLoginUri).build();
    }

    @GetMapping("/steam/callback")
    public ResponseEntity<?> steamCallback(HttpServletRequest request, Authentication authentication) {
        // Validate OpenID assertion with Steam
        boolean valid = validateOpenIdWithSteam(request);
        if (!valid) {
            return ResponseEntity.status(401).body("Invalid OpenID response from Steam");
        }

        String claimedId = request.getParameter("openid.claimed_id");
        if (claimedId == null) {
            return ResponseEntity.badRequest().body("Missing openid.claimed_id");
        }

        // claimedId format: https://steamcommunity.com/openid/id/{steamId}
        String steamId = claimedId.substring(claimedId.lastIndexOf('/') + 1);

        if (authentication != null && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            Optional<User> userOpt = userService.get(principal.getId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                SteamAccount account = SteamAccount.builder()
                        .user(user)
                        .steamId(steamId)
                        .profileUrl("https://steamcommunity.com/profiles/" + steamId)
                        .synced(false)
                        .build();

                steamAccountRepository.findByUserId(user.getId()).ifPresent(existing -> steamAccountRepository.delete(existing));
                steamAccountRepository.save(account);

                return ResponseEntity.status(302)
                        .location(URI.create(frontendBaseUrl + "/steam?steam=linked"))
                        .build();
            }
        }

        // If not authenticated, return the steamId (frontend should handle linking after login)
        return ResponseEntity.ok(steamId);
    }

    private boolean validateOpenIdWithSteam(HttpServletRequest request) {
        try {
            // Repost all received parameters with openid.mode=check_authentication
            StringBuilder postBody = new StringBuilder();
            var params = request.getParameterMap();
            for (var entry : params.entrySet()) {
                for (String value : entry.getValue()) {
                    if (postBody.length() > 0) postBody.append('&');
                    postBody.append(entry.getKey()).append('=').append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            if (postBody.length() > 0) postBody.append('&');
            postBody.append("openid.mode=check_authentication");

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            HttpEntity<String> entity = new HttpEntity<>(postBody.toString(), headers);

            ResponseEntity<String> resp = restTemplate.postForEntity("https://steamcommunity.com/openid/login", entity, String.class);
            String body = resp.getBody();
            if (body == null) return false;
            return body.contains("is_valid:true");
        } catch (Exception ex) {
            return false;
        }
    }
}
