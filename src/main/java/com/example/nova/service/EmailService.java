package com.example.nova.service;

import com.example.nova.config.PasswordResetProperties;
import com.example.nova.config.TeamUserProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final PasswordResetProperties passwordResetProperties;
    private final TeamUserProperties teamUserProperties;

    /**
     * Best-effort send: a failure here (unreachable SMTP, bad creds, etc.) is
     * logged but never propagated. /forgot-password must return the same
     * response whether or not delivery actually succeeded - otherwise the
     * response itself becomes an oracle for whether an email is registered.
     */
    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(passwordResetProperties.getFromAddress());
        message.setTo(to);
        message.setSubject("Reset your Nova password");
        message.setText(
                "We received a request to reset your Nova password.\n\n" +
                "Reset it here: " + resetLink + "\n\n" +
                "This link expires in " + passwordResetProperties.getTokenExpirationMinutes() + " minutes " +
                "and can only be used once.\n\n" +
                "If you didn't request this, you can safely ignore this email."
        );

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Best-effort send, same as {@link #sendPasswordResetEmail}: the team-user
     * account is created either way, so a delivery failure here shouldn't
     * fail the whole request - it's logged so the admin can share the
     * temp password another way if needed.
     */
    public void sendTeamUserWelcomeEmail(String to, String fullName, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(passwordResetProperties.getFromAddress());
        message.setTo(to);
        message.setSubject("You've been added to Nova");
        message.setText(
                "Hi " + fullName + ",\n\n" +
                "An admin has created a Nova account for you.\n\n" +
                "Email: " + to + "\n" +
                "Temporary password: " + tempPassword + "\n\n" +
                "Log in here: " + teamUserProperties.getLoginUrl() + "\n\n" +
                "We recommend changing this password after your first login."
        );

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send team-user welcome email to {}: {}", to, e.getMessage());
        }
    }
}
