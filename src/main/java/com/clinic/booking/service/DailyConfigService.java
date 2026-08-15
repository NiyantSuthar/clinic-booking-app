package com.clinic.booking.service;

import com.clinic.booking.entity.DailyConfig;
import com.clinic.booking.repository.DailyConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Ensures a DailyConfig row always exists for any date the app needs to
 * book against, so the booking flow never fails purely because nobody
 * pre-created today's row.
 *
 * Note on concurrency: createDefaultConfig() is NOT wrapped in
 * @Transactional at this method's level. save() and findByDate() each run
 * in their own short transaction (Spring Data's default). This is
 * deliberate - if two requests race to create the same date's row, the
 * DB's unique constraint on `date` rejects the second insert with a
 * DataIntegrityViolationException, and because that failure happens in
 * its own already-completed transaction (not one shared with this method),
 * we can safely catch it here and just re-fetch the row the other request
 * created, instead of the whole call getting stuck in a rollback-only state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyConfigService {

    private final DailyConfigRepository dailyConfigRepository;

    @Value("${clinic.default-patient-cap:45}")
    private int defaultPatientCap;

    @Value("${clinic.default-admin-cap:60}")
    private int defaultAdminCap;

    public DailyConfig getOrCreateForDate(LocalDate date) {
        return dailyConfigRepository.findByDate(date)
                .orElseGet(() -> createDefaultConfig(date));
    }

    private DailyConfig createDefaultConfig(LocalDate date) {
        try {
            DailyConfig config = DailyConfig.builder()
                    .date(date)
                    .patientCap(defaultPatientCap)
                    .adminCap(defaultAdminCap)
                    .patientBookedCount(0)
                    .adminBookedCount(0)
                    .build();
            DailyConfig saved = dailyConfigRepository.save(config);
            log.info("Created DailyConfig for {} (patientCap={}, adminCap={})", date, defaultPatientCap, defaultAdminCap);
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.debug("DailyConfig for {} was created concurrently - fetching the existing row instead", date);
            return dailyConfigRepository.findByDate(date)
                    .orElseThrow(() -> new IllegalStateException(
                            "DailyConfig missing for " + date + " after a unique-constraint race - this should not happen"));
        }
    }

    /**
     * Belt-and-suspenders safety net: guarantees today's row exists even if
     * no booking request has come in yet today. Runs at 00:05 every day.
     * The on-demand getOrCreateForDate() call (used by booking/read endpoints)
     * would create it anyway on first use - this just means it's ready
     * before the first request of the day arrives.
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void ensureTodayConfigExists() {
        getOrCreateForDate(LocalDate.now());
    }
}