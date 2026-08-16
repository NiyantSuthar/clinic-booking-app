package com.clinic.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Three possible outcomes of POST /auth/login, distinguished by these
 * two booleans (never both true at once):
 * - requiresOtp = true      -> brand-new number (or legacy pre-password
 *   account). Client should move to the OTP + set-password screen.
 * - requiresPassword = true -> this number is registered. Client should
 *   show/focus the password field and resubmit with it filled in.
 * - both false, token set   -> login succeeded outright (admin, or a
 *   correct password was already included in this same request).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginInitiateResponse {
    private boolean requiresOtp;
    private boolean requiresPassword;
    private String message;
    private String token;
    private String role;
}