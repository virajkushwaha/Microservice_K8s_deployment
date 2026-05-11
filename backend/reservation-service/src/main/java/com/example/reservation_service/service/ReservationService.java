package com.example.reservation_service.service;

import com.example.reservation_service.client.EmailClient;
import com.example.reservation_service.client.RoomClient;
import com.example.reservation_service.dto.EmailDto;
import com.example.reservation_service.dto.ReservationDto;
import com.example.reservation_service.dto.ReservationEmailRequest;
import com.example.reservation_service.dto.ReservationRequest;
import com.example.reservation_service.mapper.ReservationMapper;
import com.example.reservation_service.model.Reservation;
import com.example.reservation_service.model.ReservationStatus;
import com.example.reservation_service.repository.ReservationRepository;
import com.example.room_service.model.Room;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository repo;
    private final ReservationMapper mapper;
    private final RoomClient roomClient;
    private final EmailClient emailClient;

    @Transactional
    public ReservationDto createReservation(ReservationRequest req) {
        int totalGuests = req.getNumAdults() + req.getNumChildren();
        String assignedRoomNumber = roomClient.assignRoom(totalGuests, req.getRoomType());

        LocalTime currentTime = LocalTime.now();
        LocalDateTime checkInDateTime = req.getCheckIn().atTime(currentTime);
        LocalDateTime checkOutDateTime = req.getCheckOut().atTime(currentTime);

        Reservation reservation = new Reservation();
        reservation.setGuestName(req.getGuestName());
        reservation.setEmailId(req.getEmailId());
        reservation.setPhoneNumber(req.getPhoneNumber());
        reservation.setCheckIn(checkInDateTime);
        reservation.setCheckOut(checkOutDateTime);
        reservation.setNumAdults(req.getNumAdults());
        reservation.setNumChildren(req.getNumChildren());
        reservation.setRoomNumber(assignedRoomNumber);
        reservation.setStatus(ReservationStatus.CHECKED_IN);

        Reservation saved = repo.save(reservation);

        byte[] pdfBytes = generateReservationPdf(saved);

        EmailDto emailDto = new EmailDto(
                req.getEmailId(),
                "Your Hotel Reservation is Confirmed",
                "Dear " + req.getGuestName() + ", \n\nPlease find your reservation details attached.\n\nThank you for choosing our hotel!",
                pdfBytes,
                "reservation.pdf"
        );

        try {
            emailClient.sendEmail(emailDto);
        } catch (Exception ignored) {}

        return mapper.toDto(saved);
    }

    public byte[] generateReservationPdf(Reservation reservation) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLUE);
            Font keyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            Paragraph title = new Paragraph("🏨 Hotel Reservation Confirmation", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            document.add(new Paragraph("👤 Guest Details", sectionFont));
            document.add(createDetailTable(new String[][]{
                    {"Guest Name", reservation.getGuestName()},
                    {"Email ID", reservation.getEmailId()},
                    {"Phone", reservation.getPhoneNumber()}
            }, keyFont, valueFont));

            document.add(new Paragraph("🛏️ Reservation Details", sectionFont));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

            document.add(createDetailTable(new String[][]{
                    {"Room Number", String.valueOf(reservation.getRoomNumber())},
                    {"Adults", String.valueOf(reservation.getNumAdults())},
                    {"Children", String.valueOf(reservation.getNumChildren())},
                    {"Check-in", reservation.getCheckIn().format(formatter)},
                    {"Check-out", reservation.getCheckOut().format(formatter)},
                    {"Status", reservation.getStatus().toString()}
            }, keyFont, valueFont));

            Paragraph notes = new Paragraph("""
                Note:
                Please present this confirmation at the front desk during check-in.
                For any help, contact us at +91-7803-826-927.
                Late checkout beyond 12 PM may incur additional charges.
                """, valueFont);
            notes.setSpacingBefore(20);
            document.add(notes);

            Paragraph footer = new Paragraph("✅ Thank you for choosing Our Hotel!", sectionFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }

        return out.toByteArray();
    }

    private PdfPTable createDetailTable(String[][] data, Font keyFont, Font valueFont) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);
        try {
            table.setWidths(new float[]{2f, 5f});
        } catch (DocumentException ignored) {}

        for (String[] row : data) {
            PdfPCell keyCell = new PdfPCell(new Phrase(row[0] + ":", keyFont));
            PdfPCell valueCell = new PdfPCell(new Phrase(row[1], valueFont));
            keyCell.setBorder(Rectangle.NO_BORDER);
            valueCell.setBorder(Rectangle.NO_BORDER);
            table.addCell(keyCell);
            table.addCell(valueCell);
        }
        return table;
    }

    @Transactional
    public void checkoutByReservationCode(String reservationCode) {
        Reservation reservation = repo.findByReservationCodeAndStatus(
                        reservationCode, ReservationStatus.CHECKED_IN)
                .orElseThrow(() -> new RuntimeException("No active reservation found for this code"));

        LocalDateTime now = LocalDateTime.now();
        if (!now.toLocalTime().isBefore(LocalTime.NOON)) {
            now = now.plusDays(1);
        }

        reservation.setCheckOut(now);
        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        int nights = (int) ChronoUnit.DAYS.between(reservation.getCheckIn().toLocalDate(), now.toLocalDate());
        if (nights <= 0) nights = 1;
        reservation.setNights(nights);

        Room room = roomClient.getRoomByRoomNumber(reservation.getRoomNumber());
        double totalAmount = nights * room.getRatePerNight();
        reservation.setTotalAmount(totalAmount);

        repo.save(reservation);
        roomClient.updateRoomStatus(reservation.getRoomNumber(), "AVAILABLE");

        try {
            ReservationEmailRequest email = new ReservationEmailRequest();
            email.setToEmail(reservation.getEmailId());
            email.setSubject("Your Hotel Reservation Summary - " + reservation.getReservationCode());
            email.setGuestName(reservation.getGuestName());
            email.setReservationCode(reservation.getReservationCode());
            email.setRoomNumber(reservation.getRoomNumber());
            email.setCheckIn(reservation.getCheckIn());
            email.setCheckOut(reservation.getCheckOut());
            email.setNights(reservation.getNights());
            email.setNumAdults(reservation.getNumAdults());
            email.setNumChildren(reservation.getNumChildren());
            email.setTotalAmount(reservation.getTotalAmount());

            emailClient.sendReservationEmail(email);
        } catch (Exception ignored) {}
    }

    public List<ReservationDto> getAll() {
        return repo.findAll().stream().map(mapper::toDto).toList();
    }

    public List<Reservation> searchReservations(String guestName, String phoneNumber) {
        return repo.findByGuestNameAndPhoneNumber(guestName, phoneNumber);
    }

    public void cancelReservation(Long id) {
        Reservation r = repo.findById(id).orElseThrow(() -> new RuntimeException("Reservation not found"));
        r.setStatus(ReservationStatus.CANCELLED);
        repo.save(r);
        roomClient.updateRoomStatus(r.getRoomNumber(), "AVAILABLE");
    }
}
