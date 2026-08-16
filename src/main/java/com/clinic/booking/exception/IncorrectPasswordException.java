package com.clinic.booking.exception;

/** Wrong password on an existing, already-registered account. 401 - an auth failure, not a validation failure. */
public class IncorrectPasswordException extends RuntimeException {
    public IncorrectPasswordException(String message) {
        super(message);
    }
}