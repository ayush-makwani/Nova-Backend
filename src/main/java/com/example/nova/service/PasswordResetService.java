package com.example.nova.service;

import com.example.nova.config.PasswordResetProperties;
import com.example.nova.dto.ForgotPasswordRequest;
import com.example.nova.dto.MessageResponse;
import com.example.nova.dto.ResetPasswordRequest;
import com.example.nova.entity.PasswordResetToken;
import com.example.nova.entity.User;
import com.example.nova.exception.InvalidPasswordResetTokenException;
import com.example.nova.repository.PasswordResetTokenRepository;
import com.example.nova.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final MessageResponse GENERIC_RESPONSE =
            new MessageResponse("If an account exists for that email, a password reset link has been sent.");

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetProperties passwordResetProperties;

    /**
     * Always returns the same message and takes roughly the same code path
     * whether or not the email is registered, has an SSO-only account (no
     * local password), etc. - the response must never be usable to enumerate
     * accounts. Real work (and the email) only happens for eligible accounts.
     */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail())
                .filter(user -> user.getPassword() != null) // SSO-only accounts have nothing to reset locally
                .ifPresent(this::issueResetToken);

        return GENERIC_RESPONSE;
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidPasswordResetTokenException("Invalid or expired password reset link"));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidPasswordResetTokenException("Invalid or expired password reset link");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // A password reset should end every other active session, the same
        // way a change-of-credentials would.
        refreshTokenService.revokeAllForUser(user);

        log.info("Password reset completed for user '{}'", user.getUsername());
        return new MessageResponse("Password has been reset successfully. You can now log in with your new password.");
    }

    private void issueResetToken(User user) {
        passwordResetTokenRepository.invalidateAllUnusedForUser(user);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(generateOpaqueToken())
                .expiryDate(Instant.now().plus(passwordResetProperties.getTokenExpirationMinutes(), ChronoUnit.MINUTES))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        String resetLink = passwordResetProperties.getResetLinkBaseUrl() + "?token=" + resetToken.getToken();
        log.debug("Password reset link for '{}': {}", user.getUsername(), resetLink); // local-dev convenience only
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
