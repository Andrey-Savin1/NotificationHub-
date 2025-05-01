package ru.savin.notificationhub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import ru.savin.notificationhub.CreateNotificationData;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final NotificationService notificationService;


    @KafkaListener(topics = "notification", groupId = "notification")
    public void listen(CreateNotificationData data, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                                    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                                    @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            log.info("Received Avro message. Topic: {}, Partition: {}, Offset: {}, Data: {}",
                    topic, partition, offset, data);

            if (!StringUtils.isBlank(data.getUserId()) && data.getActionType().equals("CREATE")) {
                notificationService.createNewNotification(data);
                log.debug("Create Notification: {}", data);
            }
            if (!StringUtils.isBlank(data.getUserId()) && data.getActionType().equals("UPDATE")) {
                notificationService.updateNewNotification(data);
                log.debug("Update Notification: {}", data);
            }
        } catch (Exception e) {
            log.error("Error processing message: {}", data, e);
        }
    }
}
