package com.example.nova.exception;

public class TeamUserNotFoundException extends RuntimeException {
    public TeamUserNotFoundException(String message) {
        super(message);
    }
}
