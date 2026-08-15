package com.clinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for POST /admin/bookings - a walk-in/phone-call booking, always for today. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "phoneNumber is required")
    private String phoneNumber;
}