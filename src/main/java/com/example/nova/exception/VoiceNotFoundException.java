package com.example.nova.exception;

public class VoiceNotFoundException extends RuntimeException {
    public VoiceNotFoundException(String message) {
        super(message);
    }
}
