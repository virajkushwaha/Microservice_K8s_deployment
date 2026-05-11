package com.example.room_service.controller;

import com.example.room_service.dto.RoomDto;
import com.example.room_service.dto.RoomRequest;
import com.example.room_service.model.Room;
import com.example.room_service.model.RoomStatus;
import com.example.room_service.model.RoomType;
import com.example.room_service.repository.RoomRepository;
import com.example.room_service.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/room")
@RequiredArgsConstructor
@Slf4j
public class RoomController {
    private final RoomService service;
    private final RoomRepository roomRepository;

    @PostMapping("/add")
    public ResponseEntity<RoomDto> create(@RequestBody @Valid RoomRequest req) {
        log.info("Received request to add room: {}", req);
        return ResponseEntity.ok(service.addRoom(req));
    }

    @GetMapping("/allRooms")
    public ResponseEntity<List<RoomDto>> all() {
        log.info("Fetching all rooms");
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomDto> update(@PathVariable Long id, @RequestBody @Valid RoomRequest req) {
        log.info("Received update request for room ID {}: {}", id, req);
        return ResponseEntity.ok(service.updateRoom(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Received delete request for room ID {}", id);
        service.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available")
    public ResponseEntity<List<RoomDto>> search(@RequestParam int guests) {
        log.info("Searching available rooms for {} guests", guests);
        return ResponseEntity.ok(service.searchAvailableRooms(guests));
    }

    @PutMapping("/{roomNumber}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable String roomNumber, @RequestParam String status) {
        log.info("Updating status of room ID {} to {}", roomNumber, status);
        RoomStatus roomStatus = RoomStatus.valueOf(status);
        service.updateRoomStatus(roomNumber, roomStatus);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/assign")
    public ResponseEntity<String> assignRoom(@RequestParam int guests, @RequestParam String roomType) {
        log.info("Assigning available room of type {} for {} guests", roomType, guests);
        String roomNumber = service.assignAvailableRoom(guests, roomType);
        return ResponseEntity.ok(roomNumber);
    }

    @PutMapping("/update-price-by-type")
    public ResponseEntity<String> updateRoomPricesByType(
            @RequestParam String type,
            @RequestParam double price) {
        log.info("Updating price of all {} rooms to {}", type, price);
        RoomType roomType = RoomType.valueOf(type);
        service.updateRateByRoomType(roomType, price);
        return ResponseEntity.ok("Updated all " + roomType + " rooms to price " + price);
    }

    @GetMapping("/room-number/{roomNumber}")
    public Room getRoomByRoomNumber(@PathVariable String roomNumber) {
        return roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }
}
