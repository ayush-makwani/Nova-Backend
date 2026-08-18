package com.example.nova.exception;

public class CompanionEmailAlreadyExistsException extends RuntimeException {
    public CompanionEmailAlreadyExistsException(String message) {
        super(message);
    }
}
