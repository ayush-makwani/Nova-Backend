# Nova App — Spring Boot 3.4 / Spring Security 6 JWT + MFA Reference

A production-style authentication service demonstrating a hardened Spring
Security setup: JWT access tokens, rotating refresh tokens, TOTP-based
multi-factor authentication, account lockout, rate limiting, and strict
input validation, backed by MySQL.

## Stack

| Concern            | Choice                                             |
|---------------------|-----------------------------------------------------|
| Framework           | Spring Boot 3.4.2 / Spring Security 6.4             |
| Language / Runtime  | Java 21                                              |
| Database            | MySQL 8 (via Spring Data JPA / Hibernate)           |
| Access tokens       | JWT (jjwt 0.12, HS256)                              |
| Refresh tokens      | Opaque random tokens, persisted, rotated on use      |
| MFA                 | TOTP (RFC 6238) via `dev.samstevens.totp`, QR setup |
| Password hashing    | BCrypt, strength 12                                  |
| Rate limiting       | bucket4j, per-IP token bucket on auth endpoints      |

## Security features implemented

- **Stateless JWT authentication** — short-lived access tokens (15 min default), validated on every request via a custom `OncePerRequestFilter`.
- **Refresh token rotation** — every refresh call revokes the old token and issues a new one, limiting replay of a stolen refresh token. Refresh tokens are opaque, stored server-side, and individually revocable (`/logout`, `/logout-all`).
- **Token expiry handling** — access tokens expire quickly; refresh tokens expire and are checked for revocation/expiry on every use; expired/invalid tokens are rejected with clean 401 JSON responses (no stack traces).
- **Multi-factor authentication (TOTP)** — `/mfa/setup` issues a secret + QR code, `/mfa/enable` confirms enrolment, and login for MFA-enabled accounts requires a second `/mfa/verify` step using a short-lived (2 min) signed "challenge token" — the password step alone never issues real tokens.
- **Account lockout** — accounts lock automatically after N failed login attempts (default 5) for a configurable duration (default 15 min), then self-unlock.
- **Rate limiting** — per-IP token bucket on `/login`, `/signup`, `/refresh-token`, `/mfa/verify` to slow brute-force/credential-stuffing.
- **Password policy** — enforced via bean validation regex: 10+ chars, upper+lower+digit+special character.
- **Strict input validation** — `@Valid` DTOs for signup/login (username format, email format, length limits), centralised `GlobalExceptionHandler` returning consistent, non-leaky error bodies.
- **Generic authentication errors** — login never reveals whether the username or password was wrong (`hideUserNotFoundExceptions`, uniform "Invalid username or password" message) to prevent user enumeration.
- **Security HTTP headers** — HSTS (preload, includeSubDomains), `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, Content-Security-Policy, Referrer-Policy, Permissions-Policy, Cross-Origin-Opener/Resource-Policy.
- **CORS** — explicit allow-list configured via `app.security.cors.allowed-origins`, not `*`.
- **CSRF** — disabled deliberately because the API is stateless and uses a bearer token sent in a custom header (not a cookie), which is CSRF-immune by design; documented in code.
- **Method-level security** — `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` example on an admin-only endpoint, plus URL-level role restriction (`/api/admin/**`).
- **No secrets in source for real deployments** — the JWT secret / DB credentials in `application.yml` are placeholders read from environment variables; override them in production.
- **Enterprise SSO (SAML 2.0, optional)** — sign in via an enterprise identity provider (Okta, Azure AD/Entra ID, PingFederate, Keycloak, ...). Disabled by default; see [Enterprise SSO (SAML)](#enterprise-sso-saml) below.

## Project layout

```
src/main/java/com/example/secureapp
├── config/            SecurityConfig, SamlSsoConfig (optional), CORS, security headers, rate-limit filter, typed properties
├── controller/         AuthController (signup/login/refresh/logout/MFA), SsoController, DemoController
├── dto/                 Request/response DTOs with bean validation
├── entity/              User, Role, AuthProvider, RefreshToken (JPA)
├── exception/           Custom exceptions + GlobalExceptionHandler
├── repository/          Spring Data JPA repositories
├── security/             JwtService, JwtAuthenticationFilter, UserDetailsService, entry points, SAML handlers
└── service/              AuthService, RefreshTokenService, MfaService, SsoService
```

## Getting started

### 1. Create the database

```sql
-- see src/main/resources/schema.sql.example, or simply:
CREATE DATABASE secure_app_db CHARACTER SET utf8mb4;
CREATE USER 'secure_app_user'@'%' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON secure_app_db.* TO 'secure_app_user'@'%';
```

Hibernate is configured with `ddl-auto: update`, so tables are created
automatically on first run. Switch to `validate` + Flyway/Liquibase for
production.

### 2. Configure environment variables (recommended over editing `application.yml`)

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=secure_app_db
export DB_USERNAME=secure_app_user
export DB_PASSWORD=change_me

# Generate your own 256-bit+ base64 secret, e.g.:
# openssl rand -base64 64
export JWT_SECRET=<your-own-base64-secret>

export CORS_ALLOWED_ORIGINS=https://your-frontend.example.com
```

### 3. Run

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8111`.

### 4. Run tests

```bash
mvn test
```

### 5. Browse the API docs

Interactive Swagger UI: `http://localhost:8111/swagger-ui.html`
Raw OpenAPI spec: `http://localhost:8111/v3/api-docs`

Both are reachable without a token; each documented operation still enforces
the same JWT the live endpoint requires - click **Authorize** and paste an
access token obtained from `POST /api/auth/login` to try protected endpoints
from the UI. See [`OpenApiConfig`](src/main/java/com/example/nova/config/OpenApiConfig.java).

## API reference

All request/response bodies are JSON.

| Method | Path                     | Auth required | Description |
|--------|---------------------------|:--:|-------------|
| POST   | `/api/auth/signup`        | No  | Register a new account |
| POST   | `/api/auth/login`         | No  | Login with username + password. Returns tokens, or `mfaRequired: true` + `challengeToken` if MFA is enabled |
| POST   | `/api/auth/mfa/verify`    | No  | Step 2 of login: submit `challengeToken` + 6-digit TOTP `code` to receive tokens |
| POST   | `/api/auth/refresh-token` | No  | Exchange a valid refresh token for a new access + refresh token pair (rotation) |
| POST   | `/api/auth/logout`        | No  | Revoke a single refresh token |
| POST   | `/api/auth/logout-all`    | Yes | Revoke every refresh token for the current user (all devices) |
| POST   | `/api/auth/mfa/setup`     | Yes | Generate a new TOTP secret + QR code for enrolment |
| POST   | `/api/auth/mfa/enable`    | Yes | Confirm enrolment by submitting a valid code; enables MFA |
| POST   | `/api/auth/mfa/disable`   | Yes | Disable MFA on the account |
| GET    | `/api/auth/me`            | Yes | Current user profile |
| GET    | `/api/user/dashboard`     | Yes | Example protected endpoint (any authenticated user) |
| GET    | `/api/admin/dashboard`    | Yes (ROLE_ADMIN) | Example role-restricted endpoint |
| GET    | `/api/auth/sso/providers` | No  | List configured SAML identity providers (empty if SSO is disabled) |
| POST   | `/api/auth/sso/exchange`  | No  | Step 2 of SSO login: trade the one-time `code` from the SAML redirect for real tokens |

SAML-specific endpoints (`/saml2/authenticate/{registrationId}`, `/login/saml2/sso/{registrationId}`, `/saml2/service-provider-metadata/{registrationId}`) are provided directly by Spring Security when SSO is enabled — see below.

### Example: signup

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jdoe",
    "email": "jdoe@example.com",
    "fullName": "Jane Doe",
    "password": "Str0ng!Passw0rd"
  }'
```

### Example: login (no MFA)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{ "username": "jdoe", "password": "Str0ng!Passw0rd" }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "9f3c...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "mfaRequired": false
}
```

### Example: login (MFA enabled)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{ "username": "jdoe", "password": "Str0ng!Passw0rd" }'
# -> { "mfaRequired": true, "challengeToken": "eyJhbGciOi..." }

curl -X POST http://localhost:8080/api/auth/mfa/verify \
  -H "Content-Type: application/json" \
  -d '{ "challengeToken": "eyJhbGciOi...", "code": "123456" }'
# -> full AuthResponse with accessToken + refreshToken
```

### Example: refresh

```bash
curl -X POST http://localhost:8080/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{ "refreshToken": "9f3c..." }'
```

### Example: calling a protected endpoint

```bash
curl http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOi..."
```

## Enterprise SSO (SAML)

Optional SAML 2.0 Service Provider (SP) support so employees can sign in via
an enterprise identity provider (Okta, Azure AD/Entra ID, PingFederate,
Keycloak, ...) instead of a local password. **Disabled by default** — with
`app.security.sso.saml.enabled=false` (the default), `SamlSsoConfig` isn't
even evaluated and the app behaves exactly as it did before this feature
existed.

### How it fits into a stateless JWT API

SAML's browser-redirect handshake needs a moment of server-side state (to
correlate the IdP's response with the request that started it), which is at
odds with a stateless bearer-token API. This app resolves that by giving SAML
its own, separate `SecurityFilterChain` (`SamlSsoConfig`) that uses ordinary
sessions **only for the few seconds of the redirect dance** — the main
`/api/**` filter chain in `SecurityConfig` remains fully stateless throughout.
Once the assertion is validated, the app immediately steps back into its own
token model:

1. Browser navigates to `GET /saml2/authenticate/{registrationId}` → redirected to the IdP.
2. User authenticates at the IdP → IdP POSTs a signed SAML assertion back to `POST /login/saml2/sso/{registrationId}` (the ACS endpoint).
3. Spring Security validates the assertion's signature/audience/expiry. On success, `SamlAuthenticationSuccessHandler` finds-or-provisions the local `User` (`SsoService`) and mints a **short-lived (60s), single-use exchange code** — not a real token.
4. Browser is redirected to `app.security.sso.saml.success-redirect-uri?code=...` (your frontend).
5. Frontend immediately calls `POST /api/auth/sso/exchange` with that `code` and receives a normal `AuthResponse` (`accessToken` + `refreshToken`), exactly like `/api/auth/login`. From here on, everything works like any other session — refresh, logout, logout-all, protected endpoints.

This mirrors the existing MFA challenge-token pattern (`/mfa/verify`) rather
than putting real, long-lived tokens in a browser-visible redirect URL.

### Enabling it

1. Set `app.security.sso.saml.enabled=true` (env var `SSO_SAML_ENABLED=true`).
2. Configure at least one relying party registration — Spring Boot
   auto-configures the `RelyingPartyRegistrationRepository` bean from
   `spring.security.saml2.relyingparty.registration.<id>.*` properties.
   `application.yml` has a full commented example; the minimal metadata-URL
   form looks like:

   ```yaml
   spring:
     security:
       saml2:
         relyingparty:
           registration:
             okta:                      # -> /saml2/authenticate/okta
               assertingparty:
                 metadata-uri: https://your-org.okta.com/app/exk.../sso/saml/metadata
   ```

3. Register this app as a SAML application in your IdP. Its SP metadata is
   published at `GET /saml2/service-provider-metadata/{registrationId}` —
   most IdPs can import that URL directly.
4. If `app.security.sso.saml.enabled=true` but no registration is configured,
   the app **fails fast at startup** with a clear error instead of silently
   running without SSO.

### Account linking & JIT provisioning

- Accounts are matched by **(`registrationId`, SAML `NameID`)**, stored on
  `User.ssoRegistrationId` / `User.ssoSubjectId` — never by matching an
  asserted email address against an existing local account. An email
  attribute is only as trustworthy as the IdP that issued it; auto-linking by
  email would let a misconfigured or malicious IdP take over an existing
  password-based account.
- `app.security.sso.saml.auto-provision` (default `true`): the first time a
  given IdP subject signs in, a new local account is created automatically
  (`authProvider = SAML`, no password, role from `default-role`). Set to
  `false` to require an administrator to pre-link accounts instead.
- `email-attribute` / `full-name-attribute` control which SAML assertion
  attributes populate the new user's profile.
- SSO-provisioned accounts have `password = null` and cannot log in via
  `POST /api/auth/login` — that endpoint rejects them with the same generic
  "Invalid username or password" response used for any bad credentials
  (`AuthService.login`), and deliberately does **not** count it as a failed
  attempt, so a script can't lock an SSO user out of their real login path by
  guessing passwords against their username.

### Limitations / not implemented

- **IdP-initiated Single Logout (SLO) is not wired up.** There's no server
  session on the API side to invalidate — only revocable refresh tokens
  (`/api/auth/logout`, `/logout-all`). Bridging a SAML `LogoutRequest` to
  revoking the affected user's refresh tokens is a reasonable next step for
  a production deployment that needs it.
- The single-use exchange-code nonce store (`SsoNonceStore`) is in-memory and
  per-instance, same caveat as the existing rate limiter — back it with a
  shared cache (e.g. Redis) for a multi-instance deployment.
- No automated integration tests are included for the SAML flow (it needs a
  real or stubbed IdP to exercise end-to-end); the rest of the auth surface
  is unaffected and its existing tests continue to pass with SSO disabled.

## Production hardening checklist (beyond what's included)

- Put this service behind TLS termination (HSTS assumes HTTPS).
- Replace the in-memory rate limiter with `bucket4j-redis` for multi-instance deployments.
- Adopt Flyway/Liquibase migrations instead of `ddl-auto: update`.
- Rotate the JWT signing secret periodically and support key rollover (`kid` header) if needed.
- Add centralized audit logging for auth events (login success/failure, lockouts, MFA changes).
- Consider short-lived JWT + refresh-token-in-httpOnly-cookie if serving a first-party web frontend, to reduce XSS exposure of the refresh token.
