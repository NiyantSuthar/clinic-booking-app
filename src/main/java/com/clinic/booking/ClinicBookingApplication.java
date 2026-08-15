package com.clinic.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableAsync added this session - required for NotificationService's
 * @Async methods to actually run on a separate thread instead of
 * silently executing synchronously (a common gotcha: @Async does
 * nothing without this).
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ClinicBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicBookingApplication.class, args);
    }
}