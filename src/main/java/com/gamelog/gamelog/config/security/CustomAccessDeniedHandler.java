package com.gamelog.gamelog.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String method = request.getMethod();
        String requestURI = request.getRequestURI();
        String origin = request.getHeader("Origin");

        if (accessDeniedException instanceof InvalidCsrfTokenException) {
            logger.warn(
                    "Invalid CSRF token: method={}, uri={}, origin={}, authenticated={}, user={}",
                    method, requestURI, origin,
                    authentication != null && authentication.isAuthenticated(),
                    authentication != null ? authentication.getName() : "NONE"
            );
        } else if (accessDeniedException instanceof MissingCsrfTokenException) {
            logger.warn(
                    "Missing CSRF token: method={}, uri={}, origin={}, authenticated={}, user={}",
                    method, requestURI, origin,
                    authentication != null && authentication.isAuthenticated(),
                    authentication != null ? authentication.getName() : "NONE"
            );
        } else if (accessDeniedException instanceof CsrfException) {
            logger.warn(
                    "CSRF error: method={}, uri={}, origin={}, authenticated={}, user={}, error={}",
                    method, requestURI, origin,
                    authentication != null && authentication.isAuthenticated(),
                    authentication != null ? authentication.getName() : "NONE",
                    accessDeniedException.getMessage()
            );
        } else {
            logger.warn(
                    "Access denied: method={}, uri={}, origin={}, authenticated={}, user={}, reason={}",
                    method, requestURI, origin,
                    authentication != null && authentication.isAuthenticated(),
                    authentication != null ? authentication.getName() : "NONE",
                    accessDeniedException.getMessage()
            );
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"timestamp\":\"" + java.time.Instant.now() + "\"," +
                "\"status\":403," +
                "\"error\":\"Forbidden\"," +
                "\"message\":\"" + accessDeniedException.getMessage() + "\"," +
                "\"path\":\"" + requestURI + "\"}"
        );
    }
}
