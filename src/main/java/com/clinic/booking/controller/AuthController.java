package com.clinic.booking.controller;

import com.clinic.booking.dto.request.RequestOtpRequest;
import com.clinic.booking.dto.request.VerifyOtpRequest;
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

    /**
     * Renamed from /auth/request-otp (Session 5) - this is now the single
     * entry point for BOTH patient and admin login, matching the "one
     * login screen" design. Detection of which flow happens entirely
     * inside AuthService.login().
     */
    @PostMapping("/login")
    public ResponseEntity<LoginInitiateResponse> login(@Valid @RequestBody RequestOtpRequest request) {
        LoginInitiateResponse response = authService.login(request.getPhoneNumber(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = authService.verifyOtp(request.getPhoneNumber(), request.getOtpCode());
        return ResponseEntity.ok(response);
    }
}