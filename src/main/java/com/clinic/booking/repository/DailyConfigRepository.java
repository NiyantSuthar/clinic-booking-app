package com.clinic.booking.repository;

import com.clinic.booking.entity.DailyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyConfigRepository extends JpaRepository<DailyConfig, Long> {

    Optional<DailyConfig> findByDate(LocalDate date);

    /**
     * The core concurrency-safe operation in the whole app. This single
     * UPDATE...WHERE statement either increments the count or affects
     * zero rows - the database itself resolves any race between concurrent
     * requests for the same date, so there's no read-check-write gap for
     * two requests to both slip through. Returns the number of rows
     * affected (1 = success, 0 = cap was already reached).
     *
     * clearAutomatically = true evicts the now-stale DailyConfig from the
     * persistence context's first-level cache after this runs, so a
     * subsequent findByDate() in the same transaction re-reads the fresh
     * row instead of returning a cached pre-update copy.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DailyConfig d SET d.patientBookedCount = d.patientBookedCount + 1 " +
            "WHERE d.date = :date AND d.patientBookedCount < d.patientCap")
    int incrementPatientBookedCount(@Param("date") LocalDate date);

    /** Same as incrementPatientBookedCount, but for admin-initiated (walk-in) bookings against adminCap. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DailyConfig d SET d.adminBookedCount = d.adminBookedCount + 1 " +
            "WHERE d.date = :date AND d.adminBookedCount < d.adminCap")
    int incrementAdminBookedCount(@Param("date") LocalDate date);
}