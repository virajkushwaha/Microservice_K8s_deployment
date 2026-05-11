package com.example.reservation_service.service;

import com.example.reservation_service.client.EmailClient;
import com.example.reservation_service.client.RoomClient;
import com.example.reservation_service.dto.EmailDto;
import com.example.reservation_service.dto.ReservationDto;
import com.example.reservation_service.dto.ReservationEmailRequest;
import com.example.reservation_service.dto.ReservationRequest;
import com.example.reservation_service.mapper.ReservationMapper;
import com.example.reservation_service.model.Reservation;
import com.example.reservation_service.model.ReservationStatus;
import com.example.reservation_service.repository.ReservationRepository;
import com.example.room_service.dto.RoomDto;
import com.example.room_service.model.Room;
import com.example.room_service.model.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ReservationServiceTest {

    @Mock
    private ReservationRepository repo;

    @Mock
    private RoomClient roomClient;

    @Mock
    private EmailClient emailClient;

    @Mock
    private ReservationMapper mapper;

    @Spy
    @InjectMocks
    private ReservationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateReservation_Success() {
        // Arrange
        ReservationRequest request = new ReservationRequest();
        request.setGuestName("John Doe");
        request.setPhoneNumber("1234567890");
        request.setEmailId("john@example.com");
        request.setCheckIn(LocalDate.now());
        request.setCheckOut(LocalDate.now().plusDays(2));
        request.setNumAdults(2);
        request.setNumChildren(1);
        request.setRoomType(String.valueOf(RoomType.DELUXE));  // Use enum if possible

        String assignedRoomNumber = "101A";

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setRoomNumber(assignedRoomNumber);
        reservation.setGuestName("John Doe");
        reservation.setPhoneNumber("1234567890");
        reservation.setEmailId("john@example.com");
        reservation.setCheckIn(LocalDateTime.of(2025, 6, 28, 14, 0));
        reservation.setCheckOut(LocalDateTime.of(2025, 6, 30, 11, 0));
        reservation.setNumAdults(2);
        reservation.setNumChildren(1);
        reservation.setStatus(ReservationStatus.CHECKED_IN);

        ReservationDto dto = new ReservationDto();
        dto.setId(1L);

        // Mock dependencies
        when(roomClient.assignRoom(3, String.valueOf(RoomType.DELUXE))).thenReturn(assignedRoomNumber);
        when(repo.save(any(Reservation.class))).thenReturn(reservation);
        when(mapper.toDto(any(Reservation.class))).thenReturn(dto);

        // Act
        ReservationDto result = service.createReservation(request);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());

        // Verify interactions
        verify(roomClient).assignRoom(3, String.valueOf(RoomType.DELUXE));
        verify(repo).save(any(Reservation.class));
        verify(emailClient).sendEmail(any(EmailDto.class));
    }

    @Test
    void testCreateReservation_NoRoomsAvailable() {
        ReservationRequest request = new ReservationRequest();
        request.setNumAdults(2);
        request.setNumChildren(1);
        request.setCheckIn(LocalDate.now());
        request.setCheckOut(LocalDate.now().plusDays(1));

        when(roomClient.assignRoom(3, null)).thenThrow(new RuntimeException("No rooms available"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            service.createReservation(request);
        });

        assertEquals("No rooms available", ex.getMessage());
    }


    @Test
    void testCancelReservation_Success() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setRoomNumber("101A");

        when(repo.findById(1L)).thenReturn(Optional.of(reservation));

        service.cancelReservation(1L);

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        verify(repo).save(reservation);
        verify(roomClient).updateRoomStatus("101A", "AVAILABLE");
    }

    @Test
    void testCancelReservation_NotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cancelReservation(99L));
        assertEquals("Reservation not found", ex.getMessage());
        verify(repo, never()).save(any());
        verify(roomClient, never()).updateRoomStatus(any(), any());
    }

    @Test
    void testCheckoutByReservationCode_successful() {
        // Arrange
        String reservationCode = "RSV_12345678";
        Reservation reservation = new Reservation();
        reservation.setReservationCode(reservationCode);
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setCheckIn(LocalDateTime.now().minusDays(2));
        reservation.setRoomNumber("101");
        reservation.setEmailId("guest@example.com");
        reservation.setGuestName("John Doe");
        reservation.setNumAdults(2);
        reservation.setNumChildren(1);

        Room room = new Room();
        room.setRatePerNight(2000.0);

        when(repo.findByReservationCodeAndStatus(reservationCode, ReservationStatus.CHECKED_IN))
                .thenReturn(Optional.of(reservation));
        when(roomClient.getRoomByRoomNumber("101")).thenReturn(room);

        // Act
        service.checkoutByReservationCode(reservationCode);

        // Assert
        verify(repo).save(any(Reservation.class));
        verify(roomClient).updateRoomStatus("101", "AVAILABLE");
        verify(emailClient).sendReservationEmail(any(ReservationEmailRequest.class));
    }

    @Test
    void testCheckoutByReservationCode_notFound() {
        String reservationCode = "INVALID_CODE";

        when(repo.findByReservationCodeAndStatus(reservationCode, ReservationStatus.CHECKED_IN))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                service.checkoutByReservationCode(reservationCode));

        assertEquals("No active reservation found for this code", exception.getMessage());
    }

}
