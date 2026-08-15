package com.clinic.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * One row per calendar date. Holds both caps and both running counts
 * for that date.
 *
 * This is the hot-path concurrency table: booking a slot means an
 * atomic conditional JPQL UPDATE against patientBookedCount or
 * adminBookedCount on the row for that date (e.g.
 * "UPDATE ... SET patient_booked_count = patient_booked_count + 1
 *  WHERE date = :date AND patient_booked_count < patient_cap"),
 * with the resulting count doubling as the queue number. That logic
 * lives in the repository/service layer in a later session - this
 * entity just models the row.
 *
 * If no row exists yet for a requested date, the service layer is
 * expected to create one on read using default caps (auto-create
 * pattern), rather than requiring a pre-seeded row for every future date.
 */
@Entity
@Table(name = "daily_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    /** Self-booking limit shown to patients. */
    @Column(name = "patient_cap", nullable = false)
    private Integer patientCap;

    /** Higher hard total admin can book into for walk-ins/phone calls. */
    @Column(name = "admin_cap", nullable = false)
    private Integer adminCap;

    @Column(name = "patient_booked_count", nullable = false)
    @Builder.Default
    private Integer patientBookedCount = 0;

    @Column(name = "admin_booked_count", nullable = false)
    @Builder.Default
    private Integer adminBookedCount = 0;
}