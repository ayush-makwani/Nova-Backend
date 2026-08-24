package com.example.nova.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {

    /** How long a reset link stays valid after being requested. */
    private int tokenExpirationMinutes = 30;

    /** Frontend page that collects the new password; the token is appended as ?token=... */
    private String resetLinkBaseUrl = "http://localhost:3000/reset-password";

    /** "From" address on the reset email. */
    private String fromAddress = "lmsconnectai@gmail.com";
}
