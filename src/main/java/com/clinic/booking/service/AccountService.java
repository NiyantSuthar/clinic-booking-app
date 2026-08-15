package com.clinic.booking.service;

import com.clinic.booking.dto.response.AccountResponse;
import com.clinic.booking.entity.Account;
import com.clinic.booking.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public AccountResponse getProfile(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Authenticated account " + accountId + " not found"));
        return toResponse(account);
    }

    @Transactional
    public AccountResponse updateProfile(Long accountId, String name, String email) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Authenticated account " + accountId + " not found"));

        account.setName(name);
        account.setEmail(email);

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    @Transactional
    public void updatePushToken(Long accountId, String expoPushToken) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Authenticated account " + accountId + " not found"));
        account.setExpoPushToken(expoPushToken);
        accountRepository.save(account);
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getPhoneNumber(), account.getName(), account.getEmail());
    }
}