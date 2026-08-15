package com.clinic.booking.exception;

/** Thrown when the login identifier is neither the admin username nor a validly-shaped phone number. */
public class InvalidLoginIdentifierException extends RuntimeException {
    public InvalidLoginIdentifierException(String message) {
        super(message);
    }
}