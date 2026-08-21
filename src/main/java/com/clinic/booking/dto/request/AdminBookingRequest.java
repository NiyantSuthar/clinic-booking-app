package com.clinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** village added - was missing from the walk-in form, so admin-created patients never had one saved. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookingRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "phoneNumber is required")
    private String phoneNumber;

    @NotBlank(message = "village is required")
    private String village;
}