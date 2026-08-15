package com.clinic.booking.service;

import com.clinic.booking.dto.response.LoginInitiateResponse;
import com.clinic.booking.dto.response.VerifyOtpResponse;
import com.clinic.booking.entity.Account;
import com.clinic.booking.entity.OtpVerification;
import com.clinic.booking.exception.InvalidLoginIdentifierException;
import com.clinic.booking.exception.InvalidOtpException;
import com.clinic.booking.repository.AccountRepository;
import com.clinic.booking.repository.OtpVerificationRepository;
import com.clinic.booking.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final AccountRepository accountRepository;
    private final JwtService jwtService;

    @Value("${otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * The single entry point for both flows, matching "one login screen"
     * from the app design:
     * - identifier + matching password == the configured admin credentials
     *   -> issue an ADMIN JWT immediately, no OTP involved at all.
     * - otherwise -> treat identifier as a phone number, kick off the
     *   existing OTP-send flow (any submitted password is ignored).
     */
    public LoginInitiateResponse login(String identifier, String password) {
        if (password != null && !password.isBlank()
                && identifier.equals(adminUsername)
                && password.equals(adminPassword)) {

            String token = jwtService.generateToken("ADMIN", "ADMIN", Map.of());
            log.info("Admin login successful");
            return new LoginInitiateResponse(false, "Admin login successful.", token, "ADMIN");
        }

        if (!identifier.matches("^[0-9]{10}$")) {
            throw new InvalidLoginIdentifierException("Enter a valid 10-digit phone number, or the correct admin credentials.");
        }

        requestOtp(identifier);
        return new LoginInitiateResponse(true, "OTP sent.", null, null);
    }

    /**
     * STUB: real SMS sending gets plugged in here later (see knowledge
     * base Open Items). For now this just generates the code, saves it,
     * and logs it so you can complete the flow during development.
     */
    private void requestOtp(String phoneNumber) {
        String otpCode = String.format("%06d", RANDOM.nextInt(1_000_000));

        OtpVerification otp = OtpVerification.builder()
                .phoneNumber(phoneNumber)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                .used(false)
                .build();

        otpVerificationRepository.save(otp);

        log.info("[OTP STUB] Sending OTP {} to phone number {} (valid {} minutes)", otpCode, phoneNumber, otpExpiryMinutes);
    }

    @Transactional
    public VerifyOtpResponse verifyOtp(String phoneNumber, String otpCode) {
        OtpVerification otp = otpVerificationRepository
                .findFirstByPhoneNumberAndOtpCodeAndUsedFalseAndExpiresAtAfter(phoneNumber, otpCode, LocalDateTime.now())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired OTP."));

        otp.setUsed(true);
        otpVerificationRepository.save(otp);

        Account account = accountRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> accountRepository.save(Account.builder().phoneNumber(phoneNumber).build()));

        String token = jwtService.generateToken(
                String.valueOf(account.getId()), "PATIENT", Map.of("phone", account.getPhoneNumber()));

        return new VerifyOtpResponse(token, account.getId(), account.getPhoneNumber());
    }
}