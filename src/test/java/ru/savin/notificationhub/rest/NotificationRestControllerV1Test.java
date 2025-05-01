package ru.savin.notificationhub.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.savin.notificationhub.model.IdentityType;
import ru.savin.notificationhub.model.Notification;
import ru.savin.notificationhub.model.Status;
import ru.savin.notificationhub.repository.NotificationRepository;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static ru.savin.notificationhub.TestContainersConfig.postgres;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class NotificationRestControllerV1Test {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private NotificationRepository notificationRepository;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        // Используем специальный URL для Testcontainers
        String jdbcUrl = "jdbc:tc:postgresql:15:///testdb?TC_TMPFS=/testtmpfs:rw";
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:/db/migration-test");
    }

    @Test
    public void testNotificationSuccess() throws IOException {

        notificationRepository.save(Notification.builder()
                .createdAt(LocalDateTime.now())
                .message("Test message")
                .userUid("3f78ad09-d59e-4aa9-9f13-62fcf1aae868")
                .notificationStatus(Status.NEW)
                .subject("Create new notification")
                .createdBy(IdentityType.SYSTEM)
                .build());

        ResponseEntity<?> response = restTemplate.getForEntity("/api/v1/notification/3f78ad09-d59e-4aa9-9f13-62fcf1aae868", String.class);

        String responseBody = (String) response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        List<Notification> notifications = objectMapper.readValue(responseBody, new TypeReference<>() { });

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, notifications.size());
        assertEquals("3f78ad09-d59e-4aa9-9f13-62fcf1aae868", (notifications.get(0)).getUserUid());
    }

    @Test
    public void testNotificationFailed() throws JsonProcessingException {

        ResponseEntity<?> response = restTemplate.getForEntity("/api/v1/notification/3f78ad09-d59e-4aa9-9f13-62fcf1aae868", String.class);

        assertEquals(404, response.getStatusCode().value());
        String responseBody = (String) response.getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        String error = jsonNode.get("error").asText();
        assertEquals("User not found", error);

    }
}