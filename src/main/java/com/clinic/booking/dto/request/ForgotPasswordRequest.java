package com.clinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "phoneNumber must be a 10-digit number")
    private String phoneNumber;
}