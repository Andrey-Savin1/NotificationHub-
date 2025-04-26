package ru.savin.notificationhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.savin.notificationhub.model.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findAllByUserUid(String userId);

    Optional<Notification> findByUserUid( String userUid);

    Boolean existsByUserUid(String id);
}
