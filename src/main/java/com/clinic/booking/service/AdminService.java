package com.clinic.booking.service;

import com.clinic.booking.dto.response.AdminTodayBookingResponse;
import com.clinic.booking.entity.Account;
import com.clinic.booking.entity.Beneficiary;
import com.clinic.booking.entity.Booking;
import com.clinic.booking.enums.BookedBy;
import com.clinic.booking.repository.AccountRepository;
import com.clinic.booking.repository.BeneficiaryRepository;
import com.clinic.booking.repository.BookingRepository;
import com.clinic.booking.service.result.BookingResult;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Transactional
    public BookingResult bookWalkIn(String name, String phoneNumber, LocalDate date) {
        Account account = accountRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> accountRepository.save(Account.builder().phoneNumber(phoneNumber).build()));

        Beneficiary beneficiary = beneficiaryRepository.findByAccount_Id(account.getId()).stream()
                .filter(b -> b.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> beneficiaryRepository.save(
                        Beneficiary.builder().account(account).name(name).relation(null).build()));

        return bookingService.bookSlot(beneficiary.getId(), date, BookedBy.ADMIN, null);
    }

    @Transactional(readOnly = true)
    public List<AdminTodayBookingResponse> getTodayBookings() {
        LocalDate today = LocalDate.now();
        List<Booking> bookings = bookingRepository.findByDateOrderByQueueNumberAsc(today);

        if (bookings.isEmpty()) {
            return List.of();
        }

        List<Long> beneficiaryIds = bookings.stream().map(Booking::getBeneficiaryId).distinct().toList();
        Map<Long, Beneficiary> beneficiaryById = beneficiaryRepository.findByIdInWithAccount(beneficiaryIds).stream()
                .collect(Collectors.toMap(Beneficiary::getId, b -> b));

        return bookings.stream()
                .map(b -> {
                    Beneficiary beneficiary = beneficiaryById.get(b.getBeneficiaryId());
                    Account account = beneficiary != null ? beneficiary.getAccount() : null;
                    return new AdminTodayBookingResponse(
                            b.getId(),
                            b.getBeneficiaryId(),
                            beneficiary != null ? beneficiary.getName() : "Unknown",
                            account != null ? account.getPhoneNumber() : "Unknown",
                            account != null ? account.getVillage() : null,
                            b.getQueueNumber(),
                            b.getBookedBy()
                    );
                })
                .toList();
    }

    /**
     * Client change #3. Generates a fresh PDF from whatever's currently
     * booked at the moment this is called - reuses getTodayBookings()
     * directly, so calling this again an hour later after more bookings
     * come in produces an up-to-date file, exactly as requested.
     */
    public byte[] generateTodayPdf() {
        List<AdminTodayBookingResponse> bookings = getTodayBookings();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            document.add(new Paragraph("Today's Patient List - " + LocalDate.now(), titleFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Stream.of("Queue No", "Name", "Mobile No", "Village").forEach(col -> {
                PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
                cell.setBackgroundColor(new Color(230, 230, 230));
                cell.setPadding(6);
                table.addCell(cell);
            });

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            for (AdminTodayBookingResponse b : bookings) {
                table.addCell(new Phrase(String.valueOf(b.getQueueNumber()), cellFont));
                table.addCell(new Phrase(b.getName(), cellFont));
                table.addCell(new Phrase(b.getPhoneNumber(), cellFont));
                table.addCell(new Phrase(b.getVillage() != null ? b.getVillage() : "-", cellFont));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate today's patient PDF", e);
        }
    }
}