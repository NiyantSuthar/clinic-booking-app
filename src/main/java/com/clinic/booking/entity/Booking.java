package com.clinic.booking.entity;

import com.clinic.booking.enums.BookedBy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * A confirmed booking: one beneficiary, one date, one queue number.
 *
 * beneficiaryId is kept as a plain Long rather than a @ManyToOne on
 * purpose - the "does this beneficiary already have a booking today"
 * check and the uniqueness constraint below both key off this raw id,
 * and keeping it flat avoids pulling in a full Beneficiary fetch/join
 * for what is otherwise a simple existence check. This mirrors the
 * flat-FK convention used for the atomic-update hot path elsewhere in
 * the schema.
 *
 * The DB-level unique constraint on (beneficiary_id, date) is the real
 * backstop against double-booking a beneficiary on the same day, even
 * under concurrent requests - the same principle as the conditional
 * UPDATE on DailyConfig, just enforced via constraint instead of a
 * WHERE clause since there's no counter to race here.
 */
@Entity
@Table(
        name = "booking",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_booking_beneficiary_date",
                columnNames = {"beneficiary_id", "date"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "beneficiary_id", nullable = false)
    private Long beneficiaryId;

    @Column(nullable = false)
    private LocalDate date;

    /** The queue number for this beneficiary on this date - a byproduct of the atomic increment on DailyConfig. */
    @Column(name = "queue_number", nullable = false)
    private Integer queueNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "booked_by", nullable = false, length = 20)
    private BookedBy bookedBy;
}