package com.example.reservation_service.client;

import com.example.room_service.dto.RoomDto;
import com.example.room_service.model.Room;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "room-service")
public interface RoomClient {
    @GetMapping("/rooms/{id}")
    RoomDto getRoom(@PathVariable("id") Long id);

    @PutMapping("/room/{roomNumber}/status")
    void updateRoomStatus(@PathVariable("roomNumber") String roomNumber, @RequestParam("status") String status);

    @GetMapping("/room/available")
    List<RoomDto> getAvailableRooms(@RequestParam int guests);

    @PutMapping("/room/assign")
    String assignRoom(@RequestParam int guests, @RequestParam String roomType);

    @GetMapping("/room/room-number/{roomNumber}")
    Room getRoomByRoomNumber(@PathVariable String roomNumber);
}
