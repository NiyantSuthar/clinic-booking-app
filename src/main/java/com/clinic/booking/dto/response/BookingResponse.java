package com.clinic.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /bookings. status is a plain string ("SUCCESS",
 * "ALREADY_BOOKED", "CAP_REACHED") rather than exposing the internal
 * BookingResult sealed type directly - keeps the API contract stable
 * even if the internal result type's shape changes later.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private String status;

    /** Null unless status is SUCCESS. */
    private Integer queueNumber;

    private String message;
}