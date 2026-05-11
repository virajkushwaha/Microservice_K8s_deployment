package com.example.reservation_service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationEmailRequest {
    private String toEmail;
    private String subject;
    private String guestName;
    private String reservationCode;
    private String roomNumber;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private int nights;
    private int numAdults;
    private int numChildren;
    private double totalAmount;
}
