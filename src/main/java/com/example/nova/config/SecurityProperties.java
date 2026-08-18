package com.example.nova.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private AccountLock accountLock = new AccountLock();
    private Mfa mfa = new Mfa();
    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();
    private Sso sso = new Sso();

    @Data
    public static class AccountLock {
        private int maxFailedAttempts = 5;
        private int lockDurationMinutes = 15;
    }

    @Data
    public static class Mfa {
        private String issuer = "NovaApp";
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000");
    }

    @Data
    public static class RateLimit {
        private Login login = new Login();

        @Data
        public static class Login {
            private int capacity = 10;
            private int refillTokens = 10;
            private int refillDurationMinutes = 1;
        }
    }

    @Data
    public static class Sso {
        private Saml saml = new Saml();

        /** Enterprise single sign-on via SAML 2.0. Off by default: see SamlSsoConfig. */
        @Data
        public static class Saml {
            /**
             * Master on/off switch. When false (the default) no SAML filter chain is
             * registered and the app behaves exactly as it did before SSO existed,
             * regardless of any spring.security.saml2.relyingparty.registration.*
             * properties present.
             */
            private boolean enabled = false;

            /**
             * If true, a local account is automatically created the first time a new
             * IdP subject signs in. If false, only accounts an administrator has
             * already linked (sso_registration_id + sso_subject_id set) may sign in
             * via SSO.
             */
            private boolean autoProvision = true;

            /** Role granted to accounts created via JIT provisioning. */
            private String defaultRole = "ROLE_USER";

            /** SAML assertion attribute name to read the user's email from. */
            private String emailAttribute = "email";

            /** SAML assertion attribute name to read the user's display name from. */
            private String fullNameAttribute = "displayName";

            /** Frontend URL the browser is redirected to after a successful SAML login, with ?code=... appended. */
            private String successRedirectUri = "http://localhost:3000/sso/callback";

            /** Frontend URL the browser is redirected to after a failed SAML login, with ?error=... appended. */
            private String failureRedirectUri = "http://localhost:3000/sso/error";

            /** Optional human-friendly labels for the "Login with ..." buttons, keyed by registrationId. */
            private Map<String, String> displayNames = new HashMap<>();
        }
    }
}
