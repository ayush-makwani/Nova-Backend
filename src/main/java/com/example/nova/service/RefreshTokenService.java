package com.example.nova.service;

import com.example.nova.entity.RefreshToken;
import com.example.nova.entity.User;
import com.example.nova.exception.TokenRefreshException;
import com.example.nova.repository.RefreshTokenRepository;
import com.example.nova.security.JwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public RefreshToken createRefreshToken(User user, HttpServletRequest request) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(generateOpaqueToken())
                .expiryDate(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
                .revoked(false)
                .deviceInfo(request != null ? request.getHeader("User-Agent") : null)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verify(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            throw new TokenRefreshException("Refresh token has been revoked, please log in again");
        }
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Refresh token has expired, please log in again");
        }
        return refreshToken;
    }

    /** Rotates the refresh token: revokes the old one and issues a brand new one (mitigates replay). */
    @Transactional
    public RefreshToken rotate(RefreshToken oldToken, HttpServletRequest request) {
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);
        return createRefreshToken(oldToken.getUser(), request);
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
