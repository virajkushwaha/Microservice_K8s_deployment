package com.example.roomprice_service.client;

import com.example.roomprice_service.model.RoomType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "room-service")
public interface RoomClient {

    @PutMapping("/room/update-price-by-type")
    void updateRoomPricesByType(@RequestParam("type") String type,
                                @RequestParam("price") double price);
}

