package com.example.email_notification_service.controller;

import com.example.email_notification_service.dto.EmailDto;
import com.example.email_notification_service.dto.ReservationEmailRequest;
import com.example.email_notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody EmailDto req) {
        emailService.sendEmail(req);
        return ResponseEntity.ok("Email sent successfully to " + req.getTo());
    }

    @PostMapping("/send-reservation-summary")
    public ResponseEntity<String> sendReservationEmail(@RequestBody ReservationEmailRequest req) {
        try {
            emailService.sendReservationSummary(req);
            return ResponseEntity.ok("Email with PDF sent successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }
}
