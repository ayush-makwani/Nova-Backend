package com.example.nova.exception;

public class CompanionNotFoundException extends RuntimeException {
    public CompanionNotFoundException(String message) {
        super(message);
    }
}
