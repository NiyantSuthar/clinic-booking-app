package com.clinic.booking.dto.response;

import com.clinic.booking.enums.BookedBy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One row in the Admin "today's live list" screen. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTodayBookingResponse {
    private Long bookingId;
    private Long beneficiaryId;
    private String name;
    private String phoneNumber;
    private Integer queueNumber;
    private BookedBy bookedBy;
}