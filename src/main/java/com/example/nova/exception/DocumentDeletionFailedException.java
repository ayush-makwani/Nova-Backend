package com.example.nova.exception;

/** Thrown when a document could not be deleted from the configured S3 bucket. */
public class DocumentDeletionFailedException extends RuntimeException {
    public DocumentDeletionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
