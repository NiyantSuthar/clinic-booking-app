package com.clinic.booking.service;

import com.clinic.booking.dto.response.BeneficiaryResponse;
import com.clinic.booking.entity.Account;
import com.clinic.booking.entity.Beneficiary;
import com.clinic.booking.exception.BeneficiaryNotOwnedException;
import com.clinic.booking.repository.AccountRepository;
import com.clinic.booking.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public BeneficiaryResponse addBeneficiary(Long accountId, String name, String relation) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException("Authenticated account " + accountId + " not found - token referenced a deleted account"));

        Beneficiary saved = beneficiaryRepository.save(
                Beneficiary.builder().account(account).name(name).relation(relation).build());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> listBeneficiaries(Long accountId) {
        return beneficiaryRepository.findByAccount_Id(accountId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void removeBeneficiary(Long accountId, Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new BeneficiaryNotOwnedException("Beneficiary not found."));

        // Ownership check: without this, any logged-in account could delete
        // ANY beneficiary by guessing/incrementing ids, since the id alone
        // doesn't prove ownership - only the account_id column does.
        if (!beneficiary.getAccount().getId().equals(accountId)) {
            throw new BeneficiaryNotOwnedException("Beneficiary not found.");
        }

        beneficiaryRepository.delete(beneficiary);
    }

    private BeneficiaryResponse toResponse(Beneficiary b) {
        return new BeneficiaryResponse(b.getId(), b.getName(), b.getRelation());
    }
}