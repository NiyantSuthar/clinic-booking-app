package com.clinic.booking.controller;

import com.clinic.booking.dto.request.AdminBookingRequest;
import com.clinic.booking.dto.response.AdminTodayBookingResponse;
import com.clinic.booking.dto.response.BookingResponse;
import com.clinic.booking.service.AdminService;
import com.clinic.booking.service.result.BookingResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/today")
    public ResponseEntity<List<AdminTodayBookingResponse>> getToday() {
        return ResponseEntity.ok(adminService.getTodayBookings());
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingResponse> bookWalkIn(@Valid @RequestBody AdminBookingRequest request) {
        BookingResult result = adminService.bookWalkIn(request.getName(), request.getPhoneNumber(), LocalDate.now());

        if (result instanceof BookingResult.Success success) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new BookingResponse("SUCCESS", success.queueNumber(), "Walk-in booking confirmed."));
        }
        if (result instanceof BookingResult.AlreadyBooked) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new BookingResponse("ALREADY_BOOKED", null, "This patient already has a booking for today."));
        }
        if (result instanceof BookingResult.CapReached) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new BookingResponse("CAP_REACHED", null, "Admin cap reached for today."));
        }
        if (result instanceof BookingResult.InvalidDate invalidDate) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new BookingResponse("INVALID_DATE", null, invalidDate.message()));
        }
        if (result instanceof BookingResult.NotFound notFound) {
            // Practically unreachable for the admin flow (AdminService always
            // creates/find its own Beneficiary right before calling
            // bookSlot), but BookingResult is sealed - every case must be
            // handled for the code to compile.
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new BookingResponse("NOT_FOUND", null, notFound.message()));
        }

        throw new IllegalStateException("Unexpected BookingResult type: " + result.getClass());
    }
}