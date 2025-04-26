package ru.savin.notificationhub.restcontroller.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.savin.notificationhub.repository.NotificationRepository;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/notification")
@RequiredArgsConstructor
public class NotificationRestControllerV1 {

    private final NotificationRepository notificationRepository;


    @GetMapping("/{id}")
    public ResponseEntity<?> getAllNotificationByUserId(@PathVariable String id) {
        if (!notificationRepository.existsByUserUid(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "User not found",
                            "userId", id));
        }
        return ResponseEntity.ok(notificationRepository.findAllByUserUid(id));
    }
}
