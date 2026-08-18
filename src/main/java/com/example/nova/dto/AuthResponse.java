package com.example.nova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresInSeconds;

    /** true if MFA challenge must be completed before tokens are issued */
    private boolean mfaRequired;
    /** short-lived opaque token identifying the pending login, sent back to /mfa/verify */
    private String challengeToken;
}
