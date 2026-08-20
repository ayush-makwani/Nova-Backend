package com.example.nova.exception;

public class AwsCredentialNotFoundException extends RuntimeException {
    public AwsCredentialNotFoundException(String message) {
        super(message);
    }
}
