package com.clinic.booking.service.result;

public sealed interface BookingResult {

    record Success(int queueNumber) implements BookingResult {}

    record AlreadyBooked() implements BookingResult {}

    record CapReached() implements BookingResult {}

    record InvalidDate(String message) implements BookingResult {}

    /**
     * New this session: the beneficiaryId either doesn't exist, or exists
     * but belongs to a DIFFERENT account than the one making the request.
     * Both cases return this same generic message deliberately - telling
     * the caller "that beneficiary isn't yours" vs. "that beneficiary
     * doesn't exist" would let someone probe which beneficiary IDs are
     * real, the same reasoning already applied to OTP failures back in
     * Session 5.
     */
    record NotFound(String message) implements BookingResult {}
}