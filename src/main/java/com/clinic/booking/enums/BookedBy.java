package com.clinic.booking.enums;

/**
 * Who initiated a booking. Both patient self-booking and admin manual
 * booking go through the same underlying atomic increment logic on
 * DailyConfig - this field just records which counter (patientBookedCount
 * vs adminBookedCount) was used and which cap applied.
 */
public enum BookedBy {
    PATIENT,
    ADMIN
}