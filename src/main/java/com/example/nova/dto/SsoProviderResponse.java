package com.example.nova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Describes one configured SAML identity provider so a frontend can render "Login with X" buttons. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsoProviderResponse {
    private String registrationId;
    private String displayName;
    /** Browser-navigable URL (relative) that starts the SP-initiated SAML login redirect. */
    private String loginUrl;
}
