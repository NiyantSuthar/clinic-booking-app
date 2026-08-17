package com.clinic.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One row of the "when should I arrive" reference table - client change #2. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueScheduleEntry {
    private String queueRangeLabel; // e.g. "1-10"
    private String timeLabel;        // e.g. "9 AM - 10 AM"
}