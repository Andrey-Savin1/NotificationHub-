package ru.savin.notificationhub.mapper;

import org.mapstruct.Mapper;
import ru.savin.notificationhub.dto.NotificationDto;
import ru.savin.notificationhub.model.Notification;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDto map(Notification notification);
}
