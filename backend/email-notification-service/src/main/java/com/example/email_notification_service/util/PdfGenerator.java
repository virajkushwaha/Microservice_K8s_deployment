package com.example.email_notification_service.util;

import com.example.email_notification_service.dto.ReservationEmailRequest;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;

public class PdfGenerator {

    public static ByteArrayOutputStream generateReservationPdf(ReservationEmailRequest req) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            Paragraph title = new Paragraph("Hotel Reservation Summary", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Guest Name: " + req.getGuestName(), normalFont));
            document.add(new Paragraph("Reservation Code: " + req.getReservationCode(), normalFont));
            document.add(new Paragraph("Room Number: " + req.getRoomNumber(), normalFont));
            document.add(new Paragraph("Check-In: " + req.getCheckIn(), normalFont));
            document.add(new Paragraph("Check-Out: " + req.getCheckOut(), normalFont));
            document.add(new Paragraph("Nights Stayed: " + req.getNights(), normalFont));
            document.add(new Paragraph("Adults: " + req.getNumAdults(), normalFont));
            document.add(new Paragraph("Children: " + req.getNumChildren(), normalFont));
            document.add(new Paragraph("Total Amount: ₹" + req.getTotalAmount(), normalFont));

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Thank you for choosing our hotel!", normalFont));

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out;
    }
}