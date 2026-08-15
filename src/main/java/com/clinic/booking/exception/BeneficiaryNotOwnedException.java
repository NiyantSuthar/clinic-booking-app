package com.clinic.booking.exception;

/** Thrown when a request tries to read/delete a Beneficiary that doesn't belong to the authenticated account. */
public class BeneficiaryNotOwnedException extends RuntimeException {
    public BeneficiaryNotOwnedException(String message) {
        super(message);
    }
}