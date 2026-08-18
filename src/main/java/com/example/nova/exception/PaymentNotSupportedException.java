package com.example.nova.exception;

/** Thrown when app.companions.payment.enabled is true - real payment processing is not yet implemented. */
public class PaymentNotSupportedException extends RuntimeException {
    public PaymentNotSupportedException(String message) {
        super(message);
    }
}
