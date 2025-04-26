package ru.savin.notificationhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.savin.notificationhub.dto.CreateNotificationData;
import ru.savin.notificationhub.handler.NotificationHandler;

//{"message":"Registrationr","userId":"","email":"test@mail.ru"}
@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final NotificationHandler notificationHandler;


    @KafkaListener(topics = "notification", groupId = "notification")
    public void listen(CreateNotificationData data) {
        try {
            if (data.getUserId() == null || data.getUserId().isEmpty()) {

                notificationHandler.createNewNotification(data);
                log.debug("Create Notification: {}", data);
            } else {
                notificationHandler.updateNewNotification(data);
                log.debug("Update Notification: {}", data);
            }
        } catch (Exception e) {
            log.error("Error processing message: {}", data, e);
        }
    }
}
