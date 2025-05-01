package ru.savin.notificationhub.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.savin.notificationhub.CreateNotificationData;
import ru.savin.notificationhub.dto.NotificationDto;
import ru.savin.notificationhub.exception.NotificationException;
import ru.savin.notificationhub.mapper.NotificationMapper;
import ru.savin.notificationhub.model.IdentityType;
import ru.savin.notificationhub.model.Notification;
import ru.savin.notificationhub.model.Status;
import ru.savin.notificationhub.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.Objects;


@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;


    public Notification createNewNotification(CreateNotificationData action) {
        return notificationRepository.save(Notification.builder()
                .createdAt(LocalDateTime.now())
                .message(action.getMessage())
                .userUid(action.getUserId())
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
                .orElseThrow(() -> new NotificationException("Notification not found for user: " + action.getUserId()));
    }

    public Page<NotificationDto> getAllNotificationByUserUid(String id, int page, int size) {
        if (!notificationRepository.existsByUserUid(id)) {
            throw new NotificationException("Notifications not found for user: " + id);
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must not be less than one");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notifications = notificationRepository.findAllByUserUid(id, pageable);
        return notifications.map(notificationMapper::map);
    }

}
