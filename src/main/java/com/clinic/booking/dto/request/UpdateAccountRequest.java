package com.clinic.booking.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Both fields optional - @Email only validates format when a value is
 * actually present (null passes bean validation silently), so leaving
 * email blank is fine and won't trigger a 400.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {

    private String name;

    @Email(message = "Enter a valid email address.")
    private String email;
}