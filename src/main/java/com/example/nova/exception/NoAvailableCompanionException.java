package com.example.nova.exception;

/** Thrown creating a project when the user has no companion left unassigned to auto-link to it. */
public class NoAvailableCompanionException extends RuntimeException {
    public NoAvailableCompanionException(String message) {
        super(message);
    }
}
