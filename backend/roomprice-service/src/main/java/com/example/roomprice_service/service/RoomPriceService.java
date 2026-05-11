package com.example.roomprice_service.service;


import com.example.roomprice_service.client.RoomClient;
import com.example.roomprice_service.dto.RoomTypeUpdateRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class RoomPriceService {

    private final RoomClient roomServiceClient;
    private static final Logger logger = LoggerFactory.getLogger(RoomPriceService.class);

    @PostConstruct
    public void init() {
        logger.info("App started — basic logging works!");
    }


    public void updatePrice(RoomTypeUpdateRequest request) {
        logger.info("Updating price for room type: {} to {}", request.getRoomType(), request.getNewPrice());

        try {
            roomServiceClient.updateRoomPricesByType(
                    request.getRoomType().name(),
                    request.getNewPrice()
            );
            logger.debug("RoomClient update call completed for {}", request.getRoomType());
        } catch (Exception e) {
            logger.error("Failed to update price for room type: {}. Error: {}", request.getRoomType(), e.getMessage(), e);
            throw e; // or handle gracefully
        }
    }
}
