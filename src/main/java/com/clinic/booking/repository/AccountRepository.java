package com.clinic.booking.repository;

import com.clinic.booking.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    /** Used by the OTP login flow to find an existing account by phone number. */
    Optional<Account> findByPhoneNumber(String phoneNumber);
}