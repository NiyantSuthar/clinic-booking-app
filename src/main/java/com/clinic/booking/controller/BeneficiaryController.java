package com.clinic.booking.controller;

import com.clinic.booking.dto.request.BeneficiaryRequest;
import com.clinic.booking.dto.response.BeneficiaryResponse;
import com.clinic.booking.service.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * All three endpoints operate on "the logged-in account" via
 * @AuthenticationPrincipal Long accountId - populated by
 * JwtAuthenticationFilter from the request's Bearer token. accountId is
 * deliberately NEVER accepted as a request param/body field here - that
 * would let any authenticated caller manage another account's beneficiaries
 * just by changing an id in the request.
 */
@RestController
@RequestMapping("/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> addBeneficiary(
            @AuthenticationPrincipal Long accountId,
            @Valid @RequestBody BeneficiaryRequest request) {
        BeneficiaryResponse response = beneficiaryService.addBeneficiary(accountId, request.getName(), request.getRelation());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> listBeneficiaries(@AuthenticationPrincipal Long accountId) {
        return ResponseEntity.ok(beneficiaryService.listBeneficiaries(accountId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeBeneficiary(
            @AuthenticationPrincipal Long accountId,
            @PathVariable Long id) {
        beneficiaryService.removeBeneficiary(accountId, id);
        return ResponseEntity.noContent().build();
    }
}