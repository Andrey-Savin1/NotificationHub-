package ru.savin.notificationhub.restcontroller.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.savin.notificationhub.dto.NotificationDto;
import ru.savin.notificationhub.service.NotificationService;

@RestController
@RequestMapping(value = "/api/v1/notification")
@RequiredArgsConstructor
public class NotificationRestControllerV1 {

    private final NotificationService notificationService;


    @GetMapping("/{id}")
    public ResponseEntity<Page<NotificationDto>> getAllNotificationByUserId(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.getAllNotificationByUserUid(id, page, size));
    }
}
