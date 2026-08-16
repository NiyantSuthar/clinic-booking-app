package com.clinic.booking.exception;

/** newPassword and confirmPassword didn't match during registration/reset. 400 - a validation failure, not an auth failure. */
public class PasswordMismatchException extends RuntimeException {
    public PasswordMismatchException(String message) {
        super(message);
    }
}