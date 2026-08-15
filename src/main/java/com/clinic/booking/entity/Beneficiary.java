package com.clinic.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A person an Account can book for (self, family member, etc.).
 * This is what lets one logged-in account book multiple people on
 * the same or different days - the "one booking per beneficiary per
 * day" rule (enforced later on Booking) is keyed off this entity's id,
 * not the account's.
 *
 * Uses a proper @ManyToOne here (unlike the flat-Long pattern used for
 * hot-path concurrency fields elsewhere) because Beneficiary reads/writes
 * are never part of the atomic conditional-UPDATE race - only DailyConfig's
 * counters are.
 */
@Entity
@Table(name = "beneficiary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @NotBlank
    @Column(nullable = false)
    private String name;

    /** Optional, e.g. "Self", "Mother", "Son". */
    @Column
    private String relation;
}