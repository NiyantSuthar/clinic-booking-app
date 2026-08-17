package com.clinic.booking.dto.response;

import com.clinic.booking.enums.BookedBy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** village added this session - shown on-screen and used in the PDF export (client changes #1 and #3). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTodayBookingResponse {
    private Long bookingId;
    private Long beneficiaryId;
    private String name;
    private String phoneNumber;
    private String village;
    private Integer queueNumber;
    private BookedBy bookedBy;
}