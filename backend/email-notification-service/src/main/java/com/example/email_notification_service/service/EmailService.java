package com.example.email_notification_service.service;

import com.example.email_notification_service.dto.EmailDto;
import com.example.email_notification_service.dto.ReservationEmailRequest;
import com.example.email_notification_service.util.PdfGenerator;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(EmailDto request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(request.getBody());
            helper.setFrom("kushagragupta2127@gmail.com");

            if (request.getAttachment() != null && request.getAttachmentName() != null) {
                helper.addAttachment(request.getAttachmentName(), new ByteArrayResource(request.getAttachment()));
            }

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error sending email with attachment", e);
        }
    }

    public void sendReservationSummary(ReservationEmailRequest req) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(req.getToEmail());
        helper.setSubject(req.getSubject());

        String body = """
Dear %s,

Thank you for staying with us. Please find attached your reservation summary.

Regards,
Hotel Management
""".formatted(req.getGuestName());

        helper.setText(body);

        var pdf = PdfGenerator.generateReservationPdf(req);
        helper.addAttachment("ReservationSummary.pdf", new ByteArrayResource(pdf.toByteArray()));

        mailSender.send(message);
    }
}
