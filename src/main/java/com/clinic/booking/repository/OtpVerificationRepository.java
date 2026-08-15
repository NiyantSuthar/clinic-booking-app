package com.clinic.booking.repository;

import com.clinic.booking.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /**
     * The exact row verifyOtp() needs: matching phone + code, not already
     * used, not yet expired. If this returns empty, the OTP is wrong,
     * already used, or expired - verifyOtp() treats all three the same
     * way (one generic "invalid or expired" error) rather than leaking
     * which specific reason it failed, which would help someone brute-forcing codes.
     */
    Optional<OtpVerification> findFirstByPhoneNumberAndOtpCodeAndUsedFalseAndExpiresAtAfter(
            String phoneNumber, String otpCode, LocalDateTime now);
}