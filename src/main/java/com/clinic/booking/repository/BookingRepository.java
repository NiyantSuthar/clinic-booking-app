package com.clinic.booking.repository;

import com.clinic.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Backs the "already booked today?" check before creating a new Booking. */
    Optional<Booking> findByBeneficiaryIdAndDate(Long beneficiaryId, LocalDate date);

    /** Backs the admin "today's patients" live list, in queue order. */
    List<Booking> findByDateOrderByQueueNumberAsc(LocalDate date);

    /** Backs GET /bookings/history - all bookings across every beneficiary on one account. */
    List<Booking> findByBeneficiaryIdInOrderByDateDesc(List<Long> beneficiaryIds);
}