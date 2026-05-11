package com.example.room_service.service;

import com.example.room_service.dto.RoomDto;
import com.example.room_service.dto.RoomRequest;
import com.example.room_service.mapper.RoomMapper;
import com.example.room_service.model.Room;
import com.example.room_service.model.RoomStatus;
import com.example.room_service.model.RoomType;
import com.example.room_service.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoomServiceTest {

    @Mock
    private RoomRepository repo;

    @Mock
    private RoomMapper mapper;

    @InjectMocks
    private RoomService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addRoom_shouldSaveRoomAndReturnDto() {
        RoomRequest req = new RoomRequest();
        req.setRoomNumber("101");
        req.setRoomType(RoomType.DELUXE);
        req.setRatePerNight(1500);
        req.setCapacity(2);

        Room room = new Room();
        room.setStatus(RoomStatus.AVAILABLE);

        Room savedRoom = new Room();
        savedRoom.setId(1L);

        RoomDto dto = new RoomDto();
        dto.setId(1L);

        when(mapper.toEntity(req)).thenReturn(room);
        when(repo.save(room)).thenReturn(savedRoom);
        when(mapper.toDto(savedRoom)).thenReturn(dto);

        RoomDto result = service.addRoom(req);
        assertEquals(1L, result.getId());
    }

    @Test
    void updateRoom_shouldUpdateAndReturnDto() {
        RoomRequest req = new RoomRequest();
        req.setRoomNumber("102");
        req.setRoomType(RoomType.SUITE);
        req.setRatePerNight(2000);
        req.setCapacity(3);

        Room existingRoom = new Room();
        existingRoom.setId(1L);

        Room updatedRoom = new Room();
        updatedRoom.setId(1L);
        RoomDto dto = new RoomDto();
        dto.setId(1L);

        when(repo.findById(1L)).thenReturn(Optional.of(existingRoom));
        when(repo.save(any(Room.class))).thenReturn(updatedRoom);
        when(mapper.toDto(updatedRoom)).thenReturn(dto);

        RoomDto result = service.updateRoom(1L, req);
        assertEquals(1L, result.getId());
    }

    @Test
    void searchAvailableRooms_shouldReturnFilteredRooms() {
        Room room = new Room();
        RoomDto dto = new RoomDto();

        when(repo.findAvailableRooms(2)).thenReturn(List.of(room));
        when(mapper.toDto(room)).thenReturn(dto);

        List<RoomDto> result = service.searchAvailableRooms(2);
        assertEquals(1, result.size());
    }

    @Test
    void updateRoomStatus_shouldSetNewStatus() {
        // Arrange
        String roomNumber = "101A";
        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setStatus(RoomStatus.AVAILABLE);

        when(repo.findByRoomNumber(roomNumber)).thenReturn(Optional.of(room));

        // Act
        service.updateRoomStatus(roomNumber, RoomStatus.OCCUPIED);

        // Assert
        assertEquals(RoomStatus.OCCUPIED, room.getStatus());
        verify(repo).save(room);
    }

    @Test
    void updateRoomStatus_shouldThrowWhenRoomNotFound() {
        String roomNumber = "404Z";

        when(repo.findByRoomNumber(roomNumber)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.updateRoomStatus(roomNumber, RoomStatus.AVAILABLE));

        assertEquals("Room not found", exception.getMessage());
        verify(repo, never()).save(any());
    }

}
