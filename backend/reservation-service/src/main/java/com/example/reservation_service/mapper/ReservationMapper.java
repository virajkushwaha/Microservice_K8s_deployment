package com.example.reservation_service.mapper;

import com.example.reservation_service.dto.ReservationDto;
import com.example.reservation_service.model.Reservation;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {
    public ReservationDto toDto(Reservation r) {
        ReservationDto dto = new ReservationDto();
        BeanUtils.copyProperties(r, dto);
        return dto;
    }
}
