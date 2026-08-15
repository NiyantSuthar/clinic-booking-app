package com.clinic.booking.service;

import com.clinic.booking.dto.response.BookingHistoryItemResponse;
import com.clinic.booking.dto.response.DailyStatusResponse;
import com.clinic.booking.entity.Beneficiary;
import com.clinic.booking.entity.Booking;
import com.clinic.booking.entity.DailyConfig;
import com.clinic.booking.enums.BookedBy;
import com.clinic.booking.repository.BeneficiaryRepository;
import com.clinic.booking.repository.BookingRepository;
import com.clinic.booking.repository.DailyConfigRepository;
import com.clinic.booking.service.result.BookingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final DailyConfigRepository dailyConfigRepository;
    private final DailyConfigService dailyConfigService;
    private final BeneficiaryRepository beneficiaryRepository;
    private final NotificationService notificationService;

    @Value("${clinic.arrival-window-text:9 AM - 12 PM}")
    private String arrivalWindowText;

    private static final int MAX_DAYS_AHEAD = 6;

    /**
     * requestingAccountId is the account making the request, per the JWT -
     * REQUIRED and enforced for PATIENT bookings (ownership of beneficiaryId
     * is verified against it), and deliberately null/unused for ADMIN
     * walk-in bookings, since AdminService creates its own Beneficiary
     * under an Account it just created/found by phone - "ownership" isn't
     * a meaningful concept for that flow the same way.
     */
    @Transactional
    public BookingResult bookSlot(Long beneficiaryId, LocalDate date, BookedBy bookedBy, Long requestingAccountId) {

        LocalDate today = LocalDate.now();

        if (date.isBefore(today)) {
            return new BookingResult.InvalidDate("Cannot book a date in the past.");
        }
        if (date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            return new BookingResult.InvalidDate(
                    "Bookings can only be made up to " + (MAX_DAYS_AHEAD + 1) + " days in advance.");
        }
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return new BookingResult.InvalidDate("The clinic is closed on Sundays - please choose another date.");
        }

        // Ownership check - ONLY for patient-initiated bookings. This is
        // the actual security fix for this session: previously any
        // authenticated (or even unauthenticated) caller could pass ANY
        // beneficiaryId and book against it, regardless of who it
        // belonged to. A valid JWT alone doesn't fix that - it only
        // proves who's asking, not that they're allowed to book for
        // this specific beneficiary.
        if (bookedBy == BookedBy.PATIENT) {
            Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId).orElse(null);
            if (beneficiary == null || !beneficiary.getAccount().getId().equals(requestingAccountId)) {
                return new BookingResult.NotFound("Beneficiary not found.");
            }
        }

        dailyConfigService.getOrCreateForDate(date);

        if (bookingRepository.findByBeneficiaryIdAndDate(beneficiaryId, date).isPresent()) {
            return new BookingResult.AlreadyBooked();
        }

        int rowsUpdated = switch (bookedBy) {
            case PATIENT -> dailyConfigRepository.incrementPatientBookedCount(date);
            case ADMIN -> dailyConfigRepository.incrementAdminBookedCount(date);
        };

        if (rowsUpdated == 0) {
            return new BookingResult.CapReached();
        }

        DailyConfig updated = dailyConfigRepository.findByDate(date)
                .orElseThrow(() -> new IllegalStateException(
                        "DailyConfig unexpectedly missing for " + date + " right after updating it"));

        int queueNumber = (bookedBy == BookedBy.PATIENT)
                ? updated.getPatientBookedCount()
                : updated.getAdminBookedCount();

        try {
            bookingRepository.save(Booking.builder()
                    .beneficiaryId(beneficiaryId)
                    .date(date)
                    .queueNumber(queueNumber)
                    .bookedBy(bookedBy)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.warn("Race detected: beneficiary {} already has a booking for {} (concurrent request)", beneficiaryId, date);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new BookingResult.AlreadyBooked();
        }

        beneficiaryRepository.findById(beneficiaryId).ifPresent(beneficiary -> {
            if (beneficiary.getAccount() != null) {
                notificationService.sendBookingConfirmation(
                        beneficiary.getAccount().getExpoPushToken(), queueNumber, arrivalWindowText);
            }
        });

        return new BookingResult.Success(queueNumber);
    }

    @Transactional(readOnly = true)
    public DailyStatusResponse getDailyStatus(LocalDate date) {
        DailyConfig config = dailyConfigService.getOrCreateForDate(date);

        return new DailyStatusResponse(
                config.getDate(),
                config.getPatientBookedCount(),
                config.getPatientCap(),
                config.getPatientBookedCount() >= config.getPatientCap(),
                config.getAdminBookedCount(),
                config.getAdminCap(),
                config.getAdminBookedCount() >= config.getAdminCap()
        );
    }

    @Transactional(readOnly = true)
    public List<BookingHistoryItemResponse> getHistoryForAccount(Long accountId) {
        List<Beneficiary> beneficiaries = beneficiaryRepository.findByAccount_Id(accountId);
        if (beneficiaries.isEmpty()) {
            return List.of();
        }

        List<Long> beneficiaryIds = beneficiaries.stream().map(Beneficiary::getId).toList();
        Map<Long, String> nameById = beneficiaries.stream()
                .collect(Collectors.toMap(Beneficiary::getId, Beneficiary::getName));

        List<Booking> bookings = bookingRepository.findByBeneficiaryIdInOrderByDateDesc(beneficiaryIds);

        return bookings.stream()
                .map(b -> new BookingHistoryItemResponse(
                        b.getId(),
                        b.getBeneficiaryId(),
                        nameById.get(b.getBeneficiaryId()),
                        b.getDate(),
                        b.getQueueNumber(),
                        b.getBookedBy()
                ))
                .toList();
    }
}