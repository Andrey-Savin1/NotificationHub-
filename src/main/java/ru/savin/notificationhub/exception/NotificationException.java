package ru.savin.notificationhub.exception;

public class NotificationException extends BusinessException{


    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String errorCode, String message) {
        super(errorCode, message);
    }

    public NotificationException(Long userId) {
        super("Notification not found for user: " + userId, "NOTIFICATION_NOT_FOUND");
    }

}
