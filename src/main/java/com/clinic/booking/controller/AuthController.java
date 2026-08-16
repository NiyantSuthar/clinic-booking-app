package com.clinic.booking.controller;

import com.clinic.booking.dto.request.ForgotPasswordRequest;
import com.clinic.booking.dto.request.RequestOtpRequest;
import com.clinic.booking.dto.request.VerifyOtpAndSetPasswordRequest;
import com.clinic.booking.dto.response.LoginInitiateResponse;
import com.clinic.booking.dto.response.VerifyOtpResponse;
import com.clinic.booking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginInitiateResponse> login(@Valid @RequestBody RequestOtpRequest request) {
        LoginInitiateResponse response = authService.login(request.getPhoneNumber(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getPhoneNumber());
        return ResponseEntity.ok().build();
    }

    /**
     * Shape changed this session (was otpCode only - now also carries
     * newPassword/confirmPassword). Used for both first-time registration
     * completion and forgot-password reset.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtpAndSetPassword(@Valid @RequestBody VerifyOtpAndSetPasswordRequest request) {
        VerifyOtpResponse response = authService.verifyOtpAndSetPassword(
                request.getPhoneNumber(), request.getOtpCode(), request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok(response);
    }
}