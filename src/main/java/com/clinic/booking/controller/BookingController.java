package com.clinic.booking.controller;

import com.clinic.booking.dto.request.CreateBookingRequest;
import com.clinic.booking.dto.response.BookingHistoryItemResponse;
import com.clinic.booking.dto.response.BookingResponse;
import com.clinic.booking.dto.response.DailyStatusResponse;
import com.clinic.booking.dto.response.QueueScheduleEntry;
import com.clinic.booking.enums.BookedBy;
import com.clinic.booking.service.BookingService;
import com.clinic.booking.service.result.BookingResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal Long accountId,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResult result = bookingService.bookSlot(
                request.getBeneficiaryId(), request.getDate(), BookedBy.PATIENT, accountId);

        if (result instanceof BookingResult.Success success) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new BookingResponse("SUCCESS", success.queueNumber(), "Booking confirmed."));
        }
        if (result instanceof BookingResult.AlreadyBooked) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new BookingResponse("ALREADY_BOOKED", null, "This beneficiary already has a booking for that date."));
        }
        if (result instanceof BookingResult.CapReached) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new BookingResponse("CAP_REACHED", null, "All slots for that date are full."));
        }
        if (result instanceof BookingResult.InvalidDate invalidDate) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BookingResponse("INVALID_DATE", null, invalidDate.message()));
        }
        if (result instanceof BookingResult.NotFound notFound) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new BookingResponse("NOT_FOUND", null, notFound.message()));
        }

        throw new IllegalStateException("Unexpected BookingResult type: " + result.getClass());
    }

    @GetMapping("/status")
    public ResponseEntity<DailyStatusResponse> getStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getDailyStatus(date));
    }

    /** Client change #2 - public, same reasoning as /status: a schedule reference, no personal data. */
    @GetMapping("/queue-schedule")
    public ResponseEntity<List<QueueScheduleEntry>> getQueueSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getQueueSchedule(date));
    }

    @GetMapping("/history")
    public ResponseEntity<List<BookingHistoryItemResponse>> getHistory(@AuthenticationPrincipal Long accountId) {
        return ResponseEntity.ok(bookingService.getHistoryForAccount(accountId));
    }
}