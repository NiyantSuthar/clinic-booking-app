package com.clinic.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Used for BOTH first-time registration completion and forgot-password
 * reset - the effect (verify OTP, set a new password hash, issue a
 * token) is identical either way, so one endpoint/DTO covers both.
 *
 * confirmPassword matching newPassword is checked in the service, not
 * here - cross-field validation isn't clean to express with bean
 * validation annotations alone.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpAndSetPasswordRequest {

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "phoneNumber must be a 10-digit number")
    private String phoneNumber;

    @NotBlank(message = "otpCode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "otpCode must be a 6-digit code")
    private String otpCode;

    @NotBlank(message = "newPassword is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "confirmPassword is required")
    private String confirmPassword;
}