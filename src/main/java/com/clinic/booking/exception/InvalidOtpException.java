package com.clinic.booking.exception;

/** Thrown when the OTP is wrong, expired, or already used - all three treated identically to the caller. */
public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String message) {
        super(message);
    }
}