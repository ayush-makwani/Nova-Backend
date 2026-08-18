package com.example.nova.config;

import com.example.nova.security.SamlAuthenticationFailureHandler;
import com.example.nova.security.SamlAuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Wires up the optional Enterprise SSO / SAML 2.0 relying-party (Service
 * Provider) support, entirely separate from the main stateless-JWT API
 * filter chain in {@link SecurityConfig}.
 *
 * <h2>Why a second filter chain</h2>
 * The main API is a stateless bearer-token API (no HTTP session, CSRF
 * disabled because there's no cookie to forge). The SAML browser-redirect
 * dance is the opposite: the Service Provider (this app) must briefly hold
 * server-side state (a session) between issuing an AuthnRequest and
 * validating the IdP's response, correlated via relay state / InResponseTo.
 * Spring Security's {@code saml2Login()} DSL expects exactly that. Rather
 * than compromise the API chain's statelessness, SAML endpoints get their
 * own {@link SecurityFilterChain}, matched narrowly to {@code /saml2/**} and
 * {@code /login/saml2/**}, that uses ordinary session-backed security. Once
 * the assertion is validated, {@link SamlAuthenticationSuccessHandler}
 * bridges straight back into this app's normal JWT model (see
 * {@code SsoService}) and no session is used for anything afterwards.
 *
 * <h2>How to enable</h2>
 * <ol>
 *   <li>Set {@code app.security.sso.saml.enabled=true}.</li>
 *   <li>Configure at least one
 *       {@code spring.security.saml2.relyingparty.registration.<id>.*}
 *       (Spring Boot auto-configures a {@link RelyingPartyRegistrationRepository}
 *       bean from these - see application.yml for an example).</li>
 * </ol>
 * With {@code enabled=false} (the default) this configuration class does not
 * even get evaluated, so the app behaves exactly as it did without SSO.
 *
 * <h2>Not implemented</h2>
 * IdP-initiated Single Logout is not wired up: there is no session on the
 * API side to invalidate, only revocable refresh tokens
 * ({@code POST /api/auth/logout} / {@code /logout-all}). A production
 * deployment wanting full SLO would need to bridge a SAML LogoutRequest to
 * revoking the affected user's refresh tokens.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.security.sso.saml", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SamlSsoConfig {

    private final SamlAuthenticationSuccessHandler samlAuthenticationSuccessHandler;
    private final SamlAuthenticationFailureHandler samlAuthenticationFailureHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain samlSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<RelyingPartyRegistrationRepository> relyingPartyRegistrationRepositoryProvider) throws Exception {

        RelyingPartyRegistrationRepository relyingPartyRegistrationRepository =
                relyingPartyRegistrationRepositoryProvider.getIfAvailable();
        if (relyingPartyRegistrationRepository == null) {
            throw new IllegalStateException(
                    "app.security.sso.saml.enabled=true but no SAML relying party is configured. " +
                    "Set spring.security.saml2.relyingparty.registration.<id>.* properties " +
                    "(see the 'Enterprise SSO (SAML)' section of README.md), or set " +
                    "app.security.sso.saml.enabled=false to disable SSO.");
        }

        http
                .securityMatcher("/saml2/**", "/login/saml2/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // The ACS endpoint receives a cross-origin POST from the IdP, which
                // carries no CSRF token. Integrity/authenticity here comes from the
                // signed SAMLResponse itself (and RelayState/InResponseTo
                // correlation against the session-held AuthnRequest), not CSRF.
                .csrf(csrf -> csrf.disable())
                .saml2Login(saml2 -> saml2
                        .relyingPartyRegistrationRepository(relyingPartyRegistrationRepository)
                        .successHandler(samlAuthenticationSuccessHandler)
                        .failureHandler(samlAuthenticationFailureHandler));

        return http.build();
    }
}
