package com.clinic.booking.service;

import com.clinic.booking.dto.response.BookingHistoryItemResponse;
import com.clinic.booking.dto.response.DailyStatusResponse;
import com.clinic.booking.dto.response.QueueScheduleEntry;
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
import java.util.ArrayList;
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

    @Value("${clinic.slots-per-hour:10}")
    private int slotsPerHour;

    @Value("${clinic.arrival-start-hour:9}")
    private int arrivalStartHour;

    private static final int MAX_DAYS_AHEAD = 6;

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

    @Transactional
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

    /**
     * Client change #2 - the "when should I arrive" reference table.
     * Buckets patients into slotsPerHour-sized groups starting at
     * arrivalStartHour, sized against the SELECTED DATE'S actual
     * patientCap (not a hardcoded number) - so if a per-date cap is ever
     * changed later, this table stays accurate automatically rather than
     * needing a separate manual update.
     *
     * NOT marked readOnly - same lesson as getDailyStatus's earlier bug:
     * getOrCreateForDate() can perform a write (creating a new
     * DailyConfig row) the first time a date is requested.
     */
    @Transactional
    public List<QueueScheduleEntry> getQueueSchedule(LocalDate date) {
        DailyConfig config = dailyConfigService.getOrCreateForDate(date);
        int cap = config.getPatientCap();
        int buckets = (int) Math.ceil((double) cap / slotsPerHour);

        List<QueueScheduleEntry> schedule = new ArrayList<>();
        for (int i = 0; i < buckets; i++) {
            int startQueue = i * slotsPerHour + 1;
            int endQueue = Math.min((i + 1) * slotsPerHour, cap);
            int startHour = arrivalStartHour + i;
            int endHour = startHour + 1;
            schedule.add(new QueueScheduleEntry(
                    startQueue + " - " + endQueue,
                    formatHour(startHour) + " - " + formatHour(endHour)
            ));
        }
        return schedule;
    }

    private String formatHour(int hour24) {
        int h = hour24 % 24;
        String period = h < 12 ? "AM" : "PM";
        int h12 = h % 12;
        if (h12 == 0) h12 = 12;
        return h12 + " " + period;
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