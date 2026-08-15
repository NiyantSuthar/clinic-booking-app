package com.clinic.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /auth/login.
 *
 * Two distinct shapes depending on requiresOtp:
 * - requiresOtp = true  -> patient flow. token/role are null. Client should
 *   now call POST /auth/verify-otp with the code logged server-side (stub).
 * - requiresOtp = false -> admin flow matched. token/role are populated
 *   immediately - no OTP step happens at all for admin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginInitiateResponse {
    private boolean requiresOtp;
    private String message;
    private String token;
    private String role;
}