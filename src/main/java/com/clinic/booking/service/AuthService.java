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
    private final SmsService smsService;

    @Value("${otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    private static final SecureRandom RANDOM = new SecureRandom();

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

    public void forgotPassword(String phoneNumber) {
        if (!phoneNumber.matches("^[0-9]{10}$")) {
            throw new InvalidLoginIdentifierException("Enter a valid 10-digit phone number.");
        }
        requestOtp(phoneNumber);
    }

    /**
     * The OTP is ALWAYS saved and ALWAYS logged to the console,
     * regardless of sms.enabled - this is deliberate, not a leftover
     * stub. The console log is your dev-mode "delivery channel" (free,
     * instant, no wallet spend), and it also doubles as a safety net in
     * production if a real SMS ever silently fails to arrive - you can
     * still find the code in Render's logs if genuinely needed.
     * smsService.sendOtpSms() is what conditionally adds REAL delivery
     * on top of that, controlled by the sms.enabled flag.
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

        log.info("[OTP] Code {} for phone number {} (valid {} minutes)", otpCode, phoneNumber, otpExpiryMinutes);

        smsService.sendOtpSms(phoneNumber, otpCode);
    }

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