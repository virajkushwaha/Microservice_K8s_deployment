package com.example.room_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    private RoomType roomType; // Deluxe, Suite, etc.

    private double ratePerNight;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    private int capacity; // max occupants
}
