package com.gamelog.gamelog.config.security;

import com.gamelog.gamelog.model.User;
import com.gamelog.gamelog.service.auth.GoogleOAuthUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

import java.io.IOException;

@Component
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleOAuthUserService googleOAuthUserService;
    private final SecurityContextRepository securityContextRepository;
    private final String frontendBaseUrl;

    public GoogleOAuthSuccessHandler(
            GoogleOAuthUserService googleOAuthUserService,
            SecurityContextRepository securityContextRepository,
            @Value("${app.frontend.base-url}") String frontendBaseUrl
    ) {
        this.googleOAuthUserService = googleOAuthUserService;
        this.securityContextRepository = securityContextRepository;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            response.sendRedirect(frontendBaseUrl + "/login?oauth=google-error");
            return;
        }

        try {
            User user = googleOAuthUserService.synchronize(oidcUser);
            AppUserPrincipal principal = new AppUserPrincipal(user);

            Authentication localAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                    principal,
                    principal.getPassword(),
                    principal.getAuthorities()
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(localAuthentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            request.getSession(true);

            response.sendRedirect(frontendBaseUrl);
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            response.sendRedirect(frontendBaseUrl + "/login?oauth=google-error");
        }
    }
}