package ru.savin.notificationhub.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "message")
    private String message;

    @Column(name = "user_uid")
    private String userUid;

    @Column(name = "notification_status")
    @Enumerated(EnumType.STRING)
    private Status notificationStatus;

    @Column(name = "subject")
    private String subject;

    @Column(name = "created_by")
    @Enumerated(EnumType.STRING)
    private IdentityType createdBy;

}