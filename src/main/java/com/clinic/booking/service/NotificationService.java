package com.clinic.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Sends push notifications via Expo's push service
 * (https://exp.host/--/api/v2/push/send) rather than raw Firebase Admin
 * SDK - this avoids needing a Firebase service-account JSON file on the
 * backend at all. Expo's service still delivers through FCM under the
 * hood on Android, so this satisfies the knowledge base's "FCM, free"
 * plan without the extra native Firebase project setup.
 *
 * @Async: this method runs on a separate thread from the caller
 * (BookingService.bookSlot), so a slow or failed notification call never
 * holds up the booking transaction or the HTTP response to the client -
 * by the time this runs, the booking has already committed successfully.
 * Requires @EnableAsync on the main application class.
 *
 * All arguments are plain values (String/int), not lazily-loaded JPA
 * entities - safe to use from a different thread than the one that
 * fetched them, since no further DB access happens in here.
 */
@Service
@Slf4j
public class NotificationService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final RestTemplate restTemplate;

    public NotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Async
    public void sendBookingConfirmation(String expoPushToken, int queueNumber, String arrivalWindowText) {
        if (expoPushToken == null || expoPushToken.isBlank()) {
            // Expected for: accounts that haven't opened the app on a real
            // device build yet, walk-in bookings admin created (no app login
            // ever happened for that account), or anyone who denied permission.
            log.debug("No push token on file for this account - skipping booking confirmation notification.");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "to", expoPushToken,
                    "title", "Booking Confirmed",
                    "body", "You're #" + queueNumber + ". Arrive between " + arrivalWindowText + ".",
                    "sound", "default"
            );

            restTemplate.postForEntity(EXPO_PUSH_URL, new HttpEntity<>(body, headers), String.class);
            log.info("Sent booking confirmation push for queue #{}", queueNumber);
        } catch (Exception e) {
            // Never let a notification failure surface to the caller - the
            // booking already succeeded and committed by the time this runs.
            log.warn("Failed to send booking confirmation push: {}", e.getMessage());
        }
    }
}