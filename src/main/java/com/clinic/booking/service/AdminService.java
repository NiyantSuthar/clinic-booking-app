package com.clinic.booking.service;

import com.clinic.booking.dto.response.AdminTodayBookingResponse;
import com.clinic.booking.entity.Account;
import com.clinic.booking.entity.Beneficiary;
import com.clinic.booking.entity.Booking;
import com.clinic.booking.enums.BookedBy;
import com.clinic.booking.repository.AccountRepository;
import com.clinic.booking.repository.BeneficiaryRepository;
import com.clinic.booking.repository.BookingRepository;
import com.clinic.booking.service.result.BookingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    /**
     * village is now always written to the Account - whether it's brand
     * new (first-ever walk-in) or already exists (returning patient) -
     * since admin is re-collecting it fresh at the desk each time, this
     * keeps it current rather than only setting it once on first creation.
     */
    @Transactional
    public BookingResult bookWalkIn(
            String name,
            String phoneNumber,
            String village,
            LocalDate date) {

        Account existingAccount = accountRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> Account.builder()
                        .phoneNumber(phoneNumber)
                        .build());

        existingAccount.setVillage(village);

        Account account = accountRepository.save(existingAccount);

        Beneficiary beneficiary = beneficiaryRepository.findByAccount_Id(account.getId()).stream()
                .filter(b -> b.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> beneficiaryRepository.save(
                        Beneficiary.builder()
                                .account(account)
                                .name(name)
                                .relation(null)
                                .build()));

        return bookingService.bookSlot(
                beneficiary.getId(),
                date,
                BookedBy.ADMIN,
                null
        );
    }

    @Transactional(readOnly = true)
    public List<AdminTodayBookingResponse> getTodayBookings() {
        LocalDate today = LocalDate.now();
        List<Booking> bookings = bookingRepository.findByDateOrderByQueueNumberAsc(today);

        if (bookings.isEmpty()) {
            return List.of();
        }

        List<Long> beneficiaryIds = bookings.stream().map(Booking::getBeneficiaryId).distinct().toList();
        Map<Long, Beneficiary> beneficiaryById = beneficiaryRepository.findByIdInWithAccount(beneficiaryIds).stream()
                .collect(Collectors.toMap(Beneficiary::getId, b -> b));

        return bookings.stream()
                .map(b -> {
                    Beneficiary beneficiary = beneficiaryById.get(b.getBeneficiaryId());
                    Account account = beneficiary != null ? beneficiary.getAccount() : null;
                    return new AdminTodayBookingResponse(
                            b.getId(),
                            b.getBeneficiaryId(),
                            beneficiary != null ? beneficiary.getName() : "Unknown",
                            account != null ? account.getPhoneNumber() : "Unknown",
                            account != null ? account.getVillage() : null,
                            b.getQueueNumber(),
                            b.getBookedBy()
                    );
                })
                .toList();
    }
}