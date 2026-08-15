package com.clinic.booking.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request body for POST /bookings. This endpoint is the patient-facing
 * self-booking flow, so bookedBy is NOT part of the request - the
 * controller always passes BookedBy.PATIENT to the service. Admin's
 * manual walk-in booking gets its own endpoint in a later session
 * rather than accepting bookedBy here, so a patient can never submit
 * ADMIN and book against the higher cap.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "beneficiaryId is required")
    private Long beneficiaryId;

    @NotNull(message = "date is required")
    @FutureOrPresent(message = "date cannot be in the past")
    private LocalDate date;
}