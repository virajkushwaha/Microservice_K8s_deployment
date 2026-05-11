package com.example.roomprice_service.dto;


import com.example.roomprice_service.model.RoomType;
import lombok.Data;


@Data
public class RoomTypeUpdateRequest {
    private RoomType roomType;
    private double newPrice;
}
