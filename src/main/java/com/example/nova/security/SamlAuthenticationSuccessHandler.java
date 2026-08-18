package com.example.nova.security;

import com.example.nova.config.SecurityProperties;
import com.example.nova.service.SsoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Bridges a successful SAML assertion into this API's stateless JWT model.
 * Rather than establishing a server-side session, this mints a short-lived,
 * single-use exchange code (see JwtService#generateSsoExchangeCode) and
 * redirects the browser back to the frontend with it - the frontend then
 * trades the code for real tokens via POST /api/auth/sso/exchange. This
 * keeps SAML assertion details and long-lived tokens out of the browser
 * URL/history entirely.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SamlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final SsoService ssoService;
    private final SecurityProperties securityProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        SecurityProperties.Sso.Saml cfg = securityProperties.getSso().getSaml();
        try {
            Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
            String registrationId = principal.getRelyingPartyRegistrationId();
            String code = ssoService.handleSamlAuthentication(registrationId, principal);

            String target = UriComponentsBuilder.fromUriString(cfg.getSuccessRedirectUri())
                    .queryParam("code", code)
                    .build()
                    .toUriString();
            response.sendRedirect(target);
        } catch (Exception ex) {
            // Never leak assertion/provisioning details to the browser; log server-side only.
            log.warn("SAML SSO login could not be completed: {}", ex.getMessage());
            String target = UriComponentsBuilder.fromUriString(cfg.getFailureRedirectUri())
                    .queryParam("error", "sso_failed")
                    .build()
                    .toUriString();
            response.sendRedirect(target);
        }
    }
}
