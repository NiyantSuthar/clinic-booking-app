package com.clinic.booking.dto.response;

import com.clinic.booking.enums.BookedBy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * One row in the History tab's list. beneficiaryName is denormalized
 * in here (looked up once in the service) so the frontend doesn't have
 * to make a second call per booking just to show whose booking it is.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingHistoryItemResponse {

    private Long bookingId;
    private Long beneficiaryId;
    private String beneficiaryName;
    private LocalDate date;
    private Integer queueNumber;
    private BookedBy bookedBy;
}