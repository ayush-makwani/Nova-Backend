package com.example.nova.service;

import com.example.nova.config.PasswordResetProperties;
import com.example.nova.config.TeamUserProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateRenderer templateRenderer;
    private final PasswordResetProperties passwordResetProperties;
    private final TeamUserProperties teamUserProperties;

    /**
     * Best-effort send: a failure here (unreachable SMTP, bad creds, etc.) is
     * logged but never propagated. /forgot-password must return the same
     * response whether or not delivery actually succeeded - otherwise the
     * response itself becomes an oracle for whether an email is registered.
     */
    public void sendPasswordResetEmail(String to, String resetLink) {
        int expiryMinutes = passwordResetProperties.getTokenExpirationMinutes();

        String html = templateRenderer.renderEmail(
                "password-reset.html",
                "Reset your Nova password",
                "Choose a new password - this link expires in " + expiryMinutes + " minutes.",
                Map.of(
                        "resetLink", resetLink,
                        "expiryMinutes", String.valueOf(expiryMinutes)
                ));

        String text =
                "We received a request to reset your Nova password.\n\n" +
                "Reset it here: " + resetLink + "\n\n" +
                "This link expires in " + expiryMinutes + " minutes and can only be used once.\n\n" +
                "If you didn't request this, you can safely ignore this email.";

        send(to, "Reset your Nova password", text, html, "password reset");
    }

    /**
     * Best-effort send, same as {@link #sendPasswordResetEmail}: the team-user
     * account is created either way, so a delivery failure here shouldn't
     * fail the whole request - it's logged so the admin can share the
     * temp password another way if needed.
     */
    public void sendTeamUserWelcomeEmail(String to, String fullName, String tempPassword) {
        String html = templateRenderer.renderEmail(
                "team-user-welcome.html",
                "You've been added to Nova",
                "Your Nova account is ready - sign in with the temporary password inside.",
                Map.of(
                        "fullName", fullName,
                        "email", to,
                        "tempPassword", tempPassword,
                        "loginUrl", teamUserProperties.getLoginUrl()
                ));

        String text =
                "Hi " + fullName + ",\n\n" +
                "An admin has created a Nova account for you.\n\n" +
                "Email: " + to + "\n" +
                "Temporary password: " + tempPassword + "\n\n" +
                "Log in here: " + teamUserProperties.getLoginUrl() + "\n\n" +
                "We recommend changing this password after your first login.";

        send(to, "You've been added to Nova", text, html, "team-user welcome");
    }

    /**
     * Sends multipart/alternative - the plain-text part is not just a
     * courtesy: HTML-only mail scores worse with spam filters, and text-only
     * clients would otherwise show nothing at all.
     */
    private void send(String to, String subject, String text, String html, String kind) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(passwordResetProperties.getFromAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);

            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send {} email to {}: {}", kind, to, e.getMessage());
        }
    }
}
