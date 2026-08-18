package com.example.nova.security;

import com.example.nova.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handles a rejected/invalid SAML response (bad signature, expired
 * assertion, audience mismatch, etc.). Details are logged server-side only;
 * the browser is redirected to a generic error page.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SamlAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final SecurityProperties securityProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        log.warn("SAML authentication failed: {}", exception.getMessage());
        String target = UriComponentsBuilder.fromUriString(securityProperties.getSso().getSaml().getFailureRedirectUri())
                .queryParam("error", "sso_failed")
                .build()
                .toUriString();
        response.sendRedirect(target);
    }
}
