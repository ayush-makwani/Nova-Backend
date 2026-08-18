package com.example.nova.exception;

public class InvalidMfaCodeException extends RuntimeException {
    public InvalidMfaCodeException(String message) {
        super(message);
    }
}
