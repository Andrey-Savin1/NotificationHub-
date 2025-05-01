package ru.savin.notificationhub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.savin.notificationhub.model.Notification;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Page<Notification> findAllByUserUid(String userId, Pageable pageable);

    Optional<Notification> findByUserUid( String userUid);

    Boolean existsByUserUid(String id);
}
