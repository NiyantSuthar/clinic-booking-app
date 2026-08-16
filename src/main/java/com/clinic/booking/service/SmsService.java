package com.clinic.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Real OTP delivery via Fast2SMS's "Quick SMS" route (route=q) - the
 * no-DLT-registration option, since full DLT registration (₹5,900/year,
 * business KYC) isn't realistic for this project's scale. Uses the
 * SAME RestTemplate bean already defined in RestTemplateConfig for
 * NotificationService - no new bean needed.
 *
 * Gated behind sms.enabled: false locally by default (see
 * application.properties), so local development never sends a real SMS
 * or spends wallet credit - AuthService's console log is the only OTP
 * visibility during local testing. Render's production environment sets
 * SMS_ENABLED=true so real users get real texts.
 */
@Service
@Slf4j
public class SmsService {

    private final RestTemplate restTemplate;

    @Value("${sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${fast2sms.api-key:}")
    private String apiKey;

    public SmsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendOtpSms(String phoneNumber, String otpCode) {
        if (!smsEnabled) {
            log.debug("sms.enabled=false - skipping real SMS send, relying on console log only (local dev mode).");
            return;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("sms.enabled=true but fast2sms.api-key is not set - skipping SMS send.");
            return;
        }

        try {
            String message = "Your Clinic Booking OTP is " + otpCode + ". Valid for 5 minutes. Do not share this code.";

            String url = UriComponentsBuilder.fromHttpUrl("https://www.fast2sms.com/dev/bulkV2")
                    .queryParam("authorization", apiKey)
                    .queryParam("route", "q") // Quick SMS - no DLT registration required
                    .queryParam("message", message)
                    .queryParam("flash", 0)
                    .queryParam("numbers", phoneNumber)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            log.info("Fast2SMS request sent for {} - response: {}", phoneNumber, response);
        } catch (Exception e) {
            // Never let an SMS failure block registration/login - the OTP
            // row is already saved regardless, and the console log (below,
            // in AuthService) still shows the code for manual recovery if
            // real delivery genuinely failed.
            log.warn("Failed to send OTP SMS via Fast2SMS to {}: {}", phoneNumber, e.getMessage());
        }
    }
}