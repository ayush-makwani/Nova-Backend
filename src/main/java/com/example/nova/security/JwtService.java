package com.example.nova.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Responsible for creation and validation of stateless JWT access tokens.
 * Refresh tokens themselves are opaque random strings persisted in the DB
 * (see RefreshTokenService) so that they can be individually revoked.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey signingKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities().stream()
                .map(Object::toString)
                .toList());
        return buildToken(claims, userDetails.getUsername(), jwtProperties.getAccessTokenExpirationMs());
    }

    private String buildToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /** Parses & verifies signature/expiration without needing UserDetails up front. */
    public Claims parseClaims(String token) {
        return extractAllClaims(token);
    }

    public long getAccessTokenExpirationMs() {
        return jwtProperties.getAccessTokenExpirationMs();
    }

    // ------------------------------------------------------------------
    // Short-lived MFA "challenge" token: proves the user already supplied
    // correct username+password, without granting any API access, while
    // they complete the second authentication factor.
    // ------------------------------------------------------------------
    private static final long MFA_CHALLENGE_EXPIRATION_MS = 2 * 60 * 1000; // 2 minutes
    private static final String PURPOSE_CLAIM = "purpose";
    private static final String MFA_PURPOSE = "mfa_challenge";

    public String generateMfaChallengeToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(PURPOSE_CLAIM, MFA_PURPOSE);
        return buildToken(claims, username, MFA_CHALLENGE_EXPIRATION_MS);
    }

    public String validateAndExtractMfaChallenge(String challengeToken) {
        Claims claims = extractAllClaims(challengeToken);
        if (!MFA_PURPOSE.equals(claims.get(PURPOSE_CLAIM))) {
            throw new JwtException("Not a valid MFA challenge token");
        }
        if (claims.getExpiration().before(new Date())) {
            throw new JwtException("MFA challenge has expired, please log in again");
        }
        return claims.getSubject();
    }

    // ------------------------------------------------------------------
    // Short-lived SSO "exchange code": issued once a SAML assertion has been
    // validated (browser redirect flow), handed to the frontend, and traded
    // for real tokens via POST /api/auth/sso/exchange. Keeps the SAML
    // assertion/session details out of the browser entirely and mirrors the
    // MFA challenge-token pattern above. Single-use is enforced separately
    // via the embedded nonce + SsoNonceStore, since JWT validity alone only
    // proves authenticity/expiry, not that it hasn't already been redeemed.
    // ------------------------------------------------------------------
    private static final long SSO_EXCHANGE_CODE_EXPIRATION_MS = 60 * 1000; // 60 seconds
    private static final String SSO_PURPOSE_CLAIM = "purpose";
    private static final String SSO_EXCHANGE_PURPOSE = "sso_exchange";
    private static final String NONCE_CLAIM = "nonce";

    public String generateSsoExchangeCode(String username, String nonce) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SSO_PURPOSE_CLAIM, SSO_EXCHANGE_PURPOSE);
        claims.put(NONCE_CLAIM, nonce);
        return buildToken(claims, username, SSO_EXCHANGE_CODE_EXPIRATION_MS);
    }

    public SsoExchangeClaims validateAndExtractSsoExchange(String code) {
        Claims claims = extractAllClaims(code);
        if (!SSO_EXCHANGE_PURPOSE.equals(claims.get(SSO_PURPOSE_CLAIM))) {
            throw new JwtException("Not a valid SSO exchange code");
        }
        if (claims.getExpiration().before(new Date())) {
            throw new JwtException("SSO exchange code has expired, please sign in again");
        }
        return new SsoExchangeClaims(claims.getSubject(), (String) claims.get(NONCE_CLAIM));
    }

    public record SsoExchangeClaims(String username, String nonce) {
    }
}
