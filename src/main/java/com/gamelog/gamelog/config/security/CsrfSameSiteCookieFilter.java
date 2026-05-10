package com.gamelog.gamelog.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CsrfSameSiteCookieFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        chain.doFilter(request, response);

        CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrf != null) {
            ResponseCookie cookie = ResponseCookie.from("XSRF-TOKEN", csrf.getToken())
                    .httpOnly(false)
                    .secure(true)
                    .sameSite("None")
                    .path("/")
                    .build();
            response.setHeader("Set-Cookie", cookie.toString());
        }
    }
}