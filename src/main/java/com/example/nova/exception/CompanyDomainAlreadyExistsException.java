package com.example.nova.exception;

public class CompanyDomainAlreadyExistsException extends RuntimeException {
    public CompanyDomainAlreadyExistsException(String message) {
        super(message);
    }
}
