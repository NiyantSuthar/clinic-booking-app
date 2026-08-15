package com.clinic.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response for GET /bookings/status. Includes both patient and admin
 * numbers since this same app/API serves both roles - the patient
 * screens are simply expected to only render the patient* fields,
 * while admin screens can show the fuller picture (e.g. "42/45 patient
 * slots, but 50/60 total with walk-ins").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatusResponse {

    private LocalDate date;

    private int patientBookedCount;
    private int patientCap;
    private boolean patientSlotsFull;

    private int adminBookedCount;
    private int adminCap;
    private boolean adminSlotsFull;
}