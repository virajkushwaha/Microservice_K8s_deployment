package com.example.reservation_service.dto;

import com.example.reservation_service.model.ReservationStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReservationDto {
    private Long id;
    private String guestName;
    private String reservationCode;
    private String roomNumber;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private int numChildren;
    private int numAdults;
   private ReservationStatus status;
}
