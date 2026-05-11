package com.example.roomprice_service.controller;

import com.example.roomprice_service.dto.RoomTypeUpdateRequest;
import com.example.roomprice_service.service.RoomPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/room-price")
@RequiredArgsConstructor
public class RoomPriceController {

    private final RoomPriceService service;

    @PutMapping("/update")
    public ResponseEntity<String> updateRoomPrice(@RequestBody RoomTypeUpdateRequest request) {
        log.info("Received request to update price for room type: {} with new price: {}",
                request.getRoomType(), request.getNewPrice());

        service.updatePrice(request);

        log.info("Successfully updated price for room type: {}", request.getRoomType());
        return ResponseEntity.ok("Price updated successfully for all " + request.getRoomType() + " rooms.");
    }
}
