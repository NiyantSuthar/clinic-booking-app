package com.clinic.booking.runner;

import com.clinic.booking.entity.DailyConfig;
import com.clinic.booking.service.DailyConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Runs once on every app startup. Exists purely so Session 2 can be
 * verified by reading the console log, since no REST endpoints exist
 * yet to trigger DailyConfigService via Postman. Safe to leave in
 * permanently - it doubles as an extra guarantee that today's row
 * exists as soon as the app comes up, not just at the 00:05 scheduled time.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StartupDailyConfigRunner implements CommandLineRunner {

    private final DailyConfigService dailyConfigService;

    @Override
    public void run(String... args) {
        DailyConfig today = dailyConfigService.getOrCreateForDate(LocalDate.now());
        log.info("[Startup check] DailyConfig for {} -> patientCap={}, adminCap={}, patientBooked={}, adminBooked={}",
                today.getDate(), today.getPatientCap(), today.getAdminCap(),
                today.getPatientBookedCount(), today.getAdminBookedCount());
    }
}