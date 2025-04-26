package ru.savin.notificationhub.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.savin.notificationhub.dto.CreateNotificationData;
import ru.savin.notificationhub.model.IdentityType;
import ru.savin.notificationhub.model.Notification;
import ru.savin.notificationhub.model.Status;
import ru.savin.notificationhub.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class NotificationHandler {

    private final NotificationRepository notificationRepository;


    public Notification createNewNotification(CreateNotificationData action) {
        return notificationRepository.save(Notification.builder()
                .createdAt(LocalDateTime.now())
                .message(action.getMessage())
                .userUid(UUID.randomUUID().toString())
                .notificationStatus(Status.NEW)
                .subject("Create new notification")
                .createdBy(IdentityType.SYSTEM)
                .build());
    }

    public Notification updateNewNotification(CreateNotificationData action) {
        return notificationRepository.findByUserUid(action.getUserId()).map(value -> {
                    value.setModifiedAt(LocalDateTime.now());
                    value.setMessage(Objects.requireNonNull(action.getMessage(), "Empty message"));
                    value.setNotificationStatus(Status.COMPLETED);
                    value.setSubject("Updated notification");
                    value.setCreatedBy(IdentityType.USER_UID);
                    return notificationRepository.save(value);
                })
                .orElseThrow(() -> new RuntimeException("Notification not found for user: " + action.getUserId()));
    }
}
