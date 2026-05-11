package com.example.room_service.service;

import com.example.room_service.dto.RoomDto;
import com.example.room_service.dto.RoomRequest;
import com.example.room_service.mapper.RoomMapper;
import com.example.room_service.model.Room;
import com.example.room_service.model.RoomStatus;
import com.example.room_service.model.RoomType;
import com.example.room_service.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository repo;
    private final RoomMapper mapper;

    public RoomDto addRoom(RoomRequest req) {
        Room room = mapper.toEntity(req);
        room.setStatus(RoomStatus.AVAILABLE);
        Room saved = repo.save(room);
        return mapper.toDto(saved);
    }

    public List<RoomDto> getAll() {
        return repo.findAll().stream().map(mapper::toDto).toList();
    }

    public RoomDto updateRoom(Long id, RoomRequest req) {
        Room room = repo.findById(id).orElseThrow(() -> new RuntimeException("Room not found"));

        if (req.getRoomNumber() != null) room.setRoomNumber(req.getRoomNumber());
        if (req.getRoomType() != null) room.setRoomType(req.getRoomType());
        if (req.getRatePerNight() > 0) room.setRatePerNight(req.getRatePerNight());
        if (req.getCapacity() > 0) room.setCapacity(req.getCapacity());
        if (req.getStatus() != null) room.setStatus(req.getStatus());

        Room updated = repo.save(room);
        return mapper.toDto(updated);
    }

    public void deleteRoom(Long id) {
        repo.deleteById(id);
    }

    public List<RoomDto> searchAvailableRooms(int guests) {
        return repo.findAvailableRooms(guests).stream().map(mapper::toDto).toList();
    }

    public void updateRoomStatus(String roomNumber, RoomStatus status) {
        Room room = repo.findByRoomNumber(roomNumber).orElseThrow(() -> new RuntimeException("Room not found"));
        room.setStatus(status);
        repo.save(room);
    }

    @Transactional
    public String assignAvailableRoom(int guests, String roomType) {
        List<Room> availableRooms = repo.findByStatusAndRoomTypeAndCapacityGreaterThanEqual(
                RoomStatus.AVAILABLE, RoomType.valueOf(roomType.toUpperCase()), guests
        );

        if (availableRooms.isEmpty()) {
            throw new RuntimeException("No available rooms of type: " + roomType);
        }

        Room room = availableRooms.get(0);
        room.setStatus(RoomStatus.OCCUPIED);
        Room saved = repo.save(room);
        return saved.getRoomNumber();
    }

    @Transactional
    public void updateRateByRoomType(RoomType roomType, double price) {
        repo.updateRateByRoomType(roomType, price);
    }
}
