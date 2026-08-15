package com.clinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /auth/login.
 *
 * phoneNumber doubles as "identifier" - it's either an actual phone number
 * (patient flow) or the fixed admin username (admin flow). No @Pattern
 * constraint here anymore (Session 5 had one requiring exactly 10 digits) -
 * that would reject a non-numeric admin username. Format checking for the
 * phone-number case now happens in AuthService.login(), only once we know
 * it's NOT an admin login attempt.
 *
 * password is optional/null for the normal patient flow - only present
 * when attempting an admin login.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestOtpRequest {

    @NotBlank(message = "phoneNumber is required")
    private String phoneNumber;

    /** Only used for admin login. Null/absent for the normal patient OTP flow. */
    private String password;
}