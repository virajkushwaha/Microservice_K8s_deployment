package com.example.room_service.repository;

import com.example.room_service.model.Room;
import com.example.room_service.model.RoomStatus;
import com.example.room_service.model.RoomType;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByStatus(RoomStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.status = :status AND r.roomType = :roomType AND r.capacity >= :guests ORDER BY r.id ASC")
    List<Room> findByStatusAndRoomTypeAndCapacityGreaterThanEqual(@Param("status") RoomStatus status, @Param("roomType") RoomType roomType, @Param("guests") int capacity);

    @Query("SELECT r FROM Room r WHERE r.status = 'AVAILABLE' AND r.capacity >= :guests")
    List<Room> findAvailableRooms(@Param("guests") int guests);

    @Transactional
    @Modifying
    @Query("UPDATE Room r SET r.ratePerNight = :price WHERE r.roomType = :roomType")
    void updateRateByRoomType(@Param("roomType") RoomType roomType, @Param("price") double price);

    Optional<Room> findByRoomNumber(String roomNumber);
}
