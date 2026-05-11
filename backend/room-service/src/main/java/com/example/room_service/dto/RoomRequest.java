package com.example.room_service.dto;

import com.example.room_service.model.RoomStatus;
import com.example.room_service.model.RoomType;
import jakarta.validation.constraints.*;

import lombok.Data;

@Data
public class RoomRequest {

    @NotBlank(message = "Room number is required")
    @Pattern(regexp = "^[A-Z]\\d{3}$", message = "Room number must start with an uppercase letter followed by 3 digits (e.g., A101)")
    private String roomNumber;

    @NotNull(message = "Room type is required and must be one of: DELUXE, LUXURY, SUITE, STANDARD, EXECUTIVE")
    private RoomType roomType;

    @Positive(message = "Rate per night must be greater than 0")
    private double ratePerNight;

    private RoomStatus status; // Optional if you're overriding it to AVAILABLE in the service

    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacity;
}
