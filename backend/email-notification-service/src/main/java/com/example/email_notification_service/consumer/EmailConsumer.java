package com.example.email_notification_service.consumer;

import com.example.email_notification_service.config.RabbitMQConfig;
import com.example.email_notification_service.dto.EmailDto;
import com.example.email_notification_service.service.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailConsumer {

    @Autowired
    private EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void receiveEmail(EmailDto request){
        emailService.sendEmail(request);
    }
}
