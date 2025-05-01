package ru.savin.notificationhub.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDto {

    private Integer id;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private String message;
    
}
