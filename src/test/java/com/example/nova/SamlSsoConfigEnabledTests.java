package com.example.nova;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the optional SAML SSO wiring (SamlSsoConfig) actually stands up a
 * working filter chain when explicitly enabled with a relying party
 * registration configured, using a self-signed test certificate rather than
 * a real identity provider (see src/test/resources/saml/test-idp.crt).
 *
 * Complements {@link SecureAppApplicationTests}, which covers the default
 * (SSO disabled) context and is unaffected by this feature.
 */
@SpringBootTest(properties = {
        "app.security.sso.saml.enabled=true",
        "spring.security.saml2.relyingparty.registration.test-idp.assertingparty.entity-id=https://idp.example.org/entity",
        "spring.security.saml2.relyingparty.registration.test-idp.assertingparty.singlesignon.url=https://idp.example.org/sso",
        "spring.security.saml2.relyingparty.registration.test-idp.assertingparty.singlesignon.binding=POST",
        "spring.security.saml2.relyingparty.registration.test-idp.assertingparty.singlesignon.sign-request=false",
        "spring.security.saml2.relyingparty.registration.test-idp.assertingparty.verification.credentials[0].certificate-location=classpath:saml/test-idp.crt"
})
@ActiveProfiles("test")
class SamlSsoConfigEnabledTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void samlFilterChainAndRegistrationRepositoryAreWiredUpWhenEnabled() {
        assertThat(context.containsBean("samlSecurityFilterChain")).isTrue();
        assertThat(context.getBean(RelyingPartyRegistrationRepository.class)
                .findByRegistrationId("test-idp")).isNotNull();
    }
}
