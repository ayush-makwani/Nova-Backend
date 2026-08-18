package com.example.nova.entity;

/** How a user's identity is authenticated. */
public enum AuthProvider {
    /** Username + password (+ optional TOTP MFA), managed entirely by this app. */
    LOCAL,
    /** Just-in-time provisioned / linked via an enterprise SAML 2.0 identity provider. */
    SAML
}
