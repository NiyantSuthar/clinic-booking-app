package com.clinic.booking.controller;

import com.clinic.booking.dto.request.UpdateAccountRequest;
import com.clinic.booking.dto.request.UpdatePushTokenRequest;
import com.clinic.booking.dto.response.AccountResponse;
import com.clinic.booking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getProfile(@AuthenticationPrincipal Long accountId) {
        return ResponseEntity.ok(accountService.getProfile(accountId));
    }

    @PutMapping("/me")
    public ResponseEntity<AccountResponse> updateProfile(
            @AuthenticationPrincipal Long accountId,
            @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateProfile(accountId, request.getName(), request.getEmail()));
    }

    /** Called once the app successfully obtains an Expo push token (see pushNotifications.js on the frontend). */
    @PutMapping("/push-token")
    public ResponseEntity<Void> updatePushToken(
            @AuthenticationPrincipal Long accountId,
            @Valid @RequestBody UpdatePushTokenRequest request) {
        accountService.updatePushToken(accountId, request.getExpoPushToken());
        return ResponseEntity.ok().build();
    }
}