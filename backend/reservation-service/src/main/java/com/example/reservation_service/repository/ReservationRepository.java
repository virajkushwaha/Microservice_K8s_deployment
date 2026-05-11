package com.example.reservation_service.repository;

import com.example.reservation_service.model.Reservation;
import com.example.reservation_service.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByGuestNameAndPhoneNumberAndStatus(String guestName, String phoneNumber, ReservationStatus status);

    List<Reservation> findByGuestNameAndPhoneNumber(String guestName, String phoneNumber);

    Optional<Reservation> findByReservationCodeAndStatus(String reservationCode, ReservationStatus status);
}
