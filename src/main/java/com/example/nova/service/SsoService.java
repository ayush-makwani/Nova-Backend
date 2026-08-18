package com.example.nova.service;

import com.example.nova.config.SecurityProperties;
import com.example.nova.dto.AuthResponse;
import com.example.nova.dto.SsoProviderResponse;
import com.example.nova.entity.AuthProvider;
import com.example.nova.entity.RefreshToken;
import com.example.nova.entity.Role;
import com.example.nova.entity.User;
import com.example.nova.exception.SsoAuthenticationException;
import com.example.nova.repository.UserRepository;
import com.example.nova.security.JwtService;
import com.example.nova.security.SsoNonceStore;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Bridges Spring Security's browser-redirect SAML 2.0 login (see
 * SamlSsoConfig) into this API's own stateless JWT session model.
 *
 * Account linking is intentionally done by (registrationId, SAML NameID)
 * only, never by matching on an asserted email address: an email attribute
 * in a SAML assertion is only as trustworthy as the IdP that issued it, and
 * silently linking to an existing LOCAL account by email would let a
 * misconfigured or malicious IdP take over an existing local account.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SsoService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SsoNonceStore nonceStore;
    private final SecurityProperties securityProperties;
    private final ObjectProvider<RelyingPartyRegistrationRepository> relyingPartyRegistrationRepositoryProvider;

    /**
     * Called from the SAML success handler once the assertion has been
     * validated. Finds or (optionally) JIT-provisions the local account and
     * returns a short-lived, single-use exchange code for the frontend to
     * trade for real tokens.
     */
    @Transactional
    public String handleSamlAuthentication(String registrationId, Saml2AuthenticatedPrincipal principal) {
        SecurityProperties.Sso.Saml cfg = securityProperties.getSso().getSaml();
        String subjectId = principal.getName(); // SAML NameID

        User user = userRepository.findBySsoRegistrationIdAndSsoSubjectId(registrationId, subjectId)
                .orElse(null);

        if (user == null) {
            if (!cfg.isAutoProvision()) {
                throw new SsoAuthenticationException(
                        "No local account is linked to this identity provider account. Contact an administrator.");
            }
            user = provisionUser(registrationId, subjectId, principal, cfg);
        }

        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw new SsoAuthenticationException("This account is disabled or locked");
        }

        String nonce = UUID.randomUUID().toString();
        log.info("SAML SSO login for user '{}' via registration '{}'", user.getUsername(), registrationId);
        return jwtService.generateSsoExchangeCode(user.getUsername(), nonce);
    }

    /** Step 2 of SSO login: trade a valid, unused exchange code for a real access/refresh token pair. */
    @Transactional
    public AuthResponse exchangeCode(String code, HttpServletRequest httpRequest) {
        JwtService.SsoExchangeClaims claims;
        try {
            claims = jwtService.validateAndExtractSsoExchange(code);
        } catch (JwtException e) {
            throw new SsoAuthenticationException("SSO sign-in link is invalid or expired, please try again");
        }

        if (!nonceStore.consume(claims.nonce())) {
            throw new SsoAuthenticationException("This SSO sign-in link has already been used");
        }

        User user = userRepository.findByUsername(claims.username())
                .orElseThrow(() -> new SsoAuthenticationException("SSO sign-in link is invalid or expired, please try again"));

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, httpRequest);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresInSeconds(jwtService.getAccessTokenExpirationMs() / 1000)
                .mfaRequired(false)
                .build();
    }

    /** Lists configured SAML identity providers for the frontend to render login buttons. Empty when SSO is off. */
    public List<SsoProviderResponse> listProviders() {
        RelyingPartyRegistrationRepository repository = relyingPartyRegistrationRepositoryProvider.getIfAvailable();
        List<SsoProviderResponse> providers = new ArrayList<>();
        if (repository instanceof Iterable<?> registrations) {
            for (Object candidate : registrations) {
                RelyingPartyRegistration registration = (RelyingPartyRegistration) candidate;
                String id = registration.getRegistrationId();
                String displayName = securityProperties.getSso().getSaml().getDisplayNames().getOrDefault(id, id);
                providers.add(SsoProviderResponse.builder()
                        .registrationId(id)
                        .displayName(displayName)
                        .loginUrl("/saml2/authenticate/" + id)
                        .build());
            }
        }
        return providers;
    }

    private User provisionUser(String registrationId, String subjectId, Saml2AuthenticatedPrincipal principal,
                                SecurityProperties.Sso.Saml cfg) {
        String email = firstAttributeOrDefault(principal, cfg.getEmailAttribute(), subjectId);
        String fullName = firstAttributeOrDefault(principal, cfg.getFullNameAttribute(), email);
        String username = buildUniqueUsername(email);

        Role role;
        try {
            role = Role.valueOf(cfg.getDefaultRole());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid app.security.sso.saml.default-role '{}', falling back to ROLE_USER", cfg.getDefaultRole());
            role = Role.ROLE_USER;
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .fullName(fullName)
                .password(null)
                .authProvider(AuthProvider.SAML)
                .ssoRegistrationId(registrationId)
                .ssoSubjectId(subjectId)
                .roles(Set.of(role))
                .enabled(true)
                .accountNonLocked(true)
                .mfaEnabled(false)
                .build();

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new SsoAuthenticationException("Unable to provision an account for this identity - please try again");
        }
        log.info("JIT-provisioned new SSO user '{}' via SAML registration '{}'", username, registrationId);
        return user;
    }

    private String buildUniqueUsername(String email) {
        String base = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        base = base.replaceAll("[^a-zA-Z0-9._-]", "").toLowerCase();
        if (base.isBlank()) {
            base = "sso-user";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        String candidate = base;
        int suffix = 0;
        while (userRepository.existsByUsername(candidate)) {
            suffix++;
            candidate = base + "-" + suffix;
        }
        return candidate;
    }

    private String firstAttributeOrDefault(Saml2AuthenticatedPrincipal principal, String attributeName, String fallback) {
        String value = principal.getFirstAttribute(attributeName);
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
