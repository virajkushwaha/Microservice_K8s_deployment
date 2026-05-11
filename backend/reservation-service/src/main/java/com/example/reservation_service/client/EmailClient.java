package com.example.reservation_service.client;

import com.example.reservation_service.dto.EmailDto;
import com.example.reservation_service.dto.ReservationEmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "email-notification-service", url = "${email.service.url}")
public interface EmailClient {

    @PostMapping("/email/send")
    void sendEmail(@RequestBody EmailDto emailDto);

    @PostMapping("/email/send-reservation-summary")
    void sendReservationEmail(@RequestBody ReservationEmailRequest request);
}
