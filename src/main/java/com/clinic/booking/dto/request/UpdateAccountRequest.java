package com.clinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** village is now required (client change #1) - email is gone entirely. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {

    private String name;

    @NotBlank(message = "Village is required.")
    private String village;
}