package com.example.room_service.mapper;

import com.example.room_service.dto.RoomDto;
import com.example.room_service.dto.RoomRequest;
import com.example.room_service.model.Room;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {
    public RoomDto toDto(Room room) {
        RoomDto dto = new RoomDto();
        BeanUtils.copyProperties(room, dto);
        return dto;
    }

    public Room toEntity(RoomRequest req) {
        Room room = new Room();
        BeanUtils.copyProperties(req, room);
        return room;
    }
}
