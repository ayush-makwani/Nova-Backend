package com.example.nova.exception;

/** Thrown when a SAML SSO login cannot be completed (no linked account, expired/reused exchange code, etc.). */
public class SsoAuthenticationException extends RuntimeException {
    public SsoAuthenticationException(String message) {
        super(message);
    }
}
