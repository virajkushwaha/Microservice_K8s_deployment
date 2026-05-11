package com.example.reservation_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomNumber;

    private String guestName;
    private String phoneNumber;
    private String emailId;
    private String reservationCode;

    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private int numChildren;
    private int numAdults;
    private int nights;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private double totalAmount;

    @PrePersist
    public void onCreate() {
        // Generate reservation code
        this.reservationCode = "RSV_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Calculate total nights
        if (checkIn != null && checkOut != null && !checkOut.isBefore(checkIn)) {
            this.nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        } else {
            this.nights = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        // Recalculate nights during updates as well
        if (checkIn != null && checkOut != null && !checkOut.isBefore(checkIn)) {
            this.nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        } else {
            this.nights = 0;
        }
    }
}
