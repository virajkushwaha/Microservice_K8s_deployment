package com.example.room_service.dto;

import com.example.room_service.model.RoomStatus;
import com.example.room_service.model.RoomType;
import lombok.Data;

@Data
public class RoomDto {
    private Long id;
    private String roomNumber;
    private RoomType roomType;
    private double ratePerNight;
    private RoomStatus status;
    private int capacity;
}
