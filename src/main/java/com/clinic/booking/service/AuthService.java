package com.clinic.booking.service;

import com.clinic.booking.dto.response.LoginInitiateResponse;
import com.clinic.booking.dto.response.VerifyOtpResponse;
import com.clinic.booking.entity.Account;
import com.clinic.booking.entity.OtpVerification;
import com.clinic.booking.exception.IncorrectPasswordException;
import com.clinic.booking.exception.InvalidLoginIdentifierException;
import com.clinic.booking.exception.InvalidOtpException;
import com.clinic.booking.exception.PasswordMismatchException;
import com.clinic.booking.repository.AccountRepository;
import com.clinic.booking.repository.OtpVerificationRepository;
import com.clinic.booking.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final AccountRepository accountRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * The single entry point. Branches three ways:
     * 1. Admin credentials match exactly -> ADMIN token immediately.
     * 2. A registered account (has a passwordHash) -> validate the
     *    supplied password, or ask for it if it wasn't sent yet.
     * 3. No account, or an account with no passwordHash yet (brand-new
     *    number, or a leftover pre-password-era account) -> fire an OTP
     *    to begin registration. Any password typed in this branch is
     *    deliberately ignored - there's nothing to check it against yet.
     */
    public LoginInitiateResponse login(String identifier, String password) {
        if (password != null && !password.isBlank()
                && identifier.equals(adminUsername)
                && password.equals(adminPassword)) {
            String token = jwtService.generateToken("ADMIN", "ADMIN", Map.of());
            log.info("Admin login successful");
            return new LoginInitiateResponse(false, false, "Admin login successful.", token, "ADMIN");
        }

        if (!identifier.matches("^[0-9]{10}$")) {
            throw new InvalidLoginIdentifierException("Enter a valid 10-digit phone number, or the correct admin credentials.");
        }

        Optional<Account> existing = accountRepository.findByPhoneNumber(identifier);

        if (existing.isPresent() && existing.get().getPasswordHash() != null) {
            Account account = existing.get();

            if (password == null || password.isBlank()) {
                return new LoginInitiateResponse(false, true, "This number is registered - enter your password.", null, null);
            }

            if (!passwordEncoder.matches(password, account.getPasswordHash())) {
                throw new IncorrectPasswordException("Incorrect password.");
            }

            String token = jwtService.generateToken(
                    String.valueOf(account.getId()), "PATIENT", Map.of("phone", account.getPhoneNumber()));
            return new LoginInitiateResponse(false, false, "Login successful.", token, "PATIENT");
        }

        requestOtp(identifier);
        return new LoginInitiateResponse(true, false, "OTP sent to complete registration.", null, null);
    }

    /** Always fires an OTP, regardless of whether a password already exists - this IS the recovery path. */
    public void forgotPassword(String phoneNumber) {
        if (!phoneNumber.matches("^[0-9]{10}$")) {
            throw new InvalidLoginIdentifierException("Enter a valid 10-digit phone number.");
        }
        requestOtp(phoneNumber);
    }

    /**
     * STUB - see knowledge base Open Items. Logs the code instead of
     * sending a real SMS. Real Fast2SMS integration is the very next
     * session - this method is the one place that call gets added,
     * nothing else in this file changes when that happens.
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

    /** Handles BOTH first-time registration completion and forgot-password reset - identical effect either way. */
    @Transactional
    public VerifyOtpResponse verifyOtpAndSetPassword(String phoneNumber, String otpCode, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new PasswordMismatchException("Passwords do not match.");
        }

        OtpVerification otp = otpVerificationRepository
                .findFirstByPhoneNumberAndOtpCodeAndUsedFalseAndExpiresAtAfter(phoneNumber, otpCode, LocalDateTime.now())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired OTP."));

        otp.setUsed(true);
        otpVerificationRepository.save(otp);

        Account account = accountRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> Account.builder().phoneNumber(phoneNumber).build());

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        Account saved = accountRepository.save(account);

        String token = jwtService.generateToken(
                String.valueOf(saved.getId()), "PATIENT", Map.of("phone", saved.getPhoneNumber()));

        return new VerifyOtpResponse(token, saved.getId(), saved.getPhoneNumber());
    }
}