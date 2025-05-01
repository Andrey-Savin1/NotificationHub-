package ru.savin.notificationhub;

import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.util.GreenMail;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import ru.savin.notificationhub.service.NotificationService;
import ru.savin.notificationhub.repository.NotificationRepository;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.savin.notificationhub.TestContainersConfig.*;
import static ru.savin.notificationhub.util.KafkaTestUtils.createConsumer;
import static ru.savin.notificationhub.util.KafkaTestUtils.createProducer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class KafkaConsumerServiceIntegrationTest {

    @Value("${themeOfEmail}")
    private String themeOfEmail;
    private final GreenMail greenMail;
    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Используем специальный URL для Testcontainers
        String jdbcUrl = "jdbc:tc:postgresql:15:///testdb?TC_TMPFS=/testtmpfs:rw";
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:/db/migration-test");

        registry.add("spring.kafka.bootstrap-servers", TestContainersConfig.kafkaTest::getBootstrapServers);
        registry.add("kafka.consumer.enabled", () -> "true");
        registry.add("spring.kafka.producer.properties.schema.registry.url", () ->
                "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081)));
        registry.add("spring.kafka.consumer.properties.schema.registry.url", () ->
                "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081)));
        registry.add("spring.kafka.properties.schema.registry.url", () ->
                "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081)));
        registry.add("spring.kafka.properties.schema.registry.ssl.endpoint.identification.algorithm", () -> "");
        registry.add("spring.kafka.properties.schema.registry.basic.auth.user.info", () -> "");
    }

    @BeforeAll
    static void setUp() {
        //Регистрация схемы (при необходимости)
//         registerSchema();
    }

    @BeforeEach
    void clean() throws FolderException {
        //Очищаем почтовый ящик перед каждым тестом
        greenMail.purgeEmailFromAllMailboxes();
    }

    @Test
    @DisplayName("Проверяем кейс для добавления нового уведомления")
    void CreateNewNotificationTest() {

        // 1. Подготовка тестового сообщения
        CreateNotificationData testData = CreateNotificationData.newBuilder()
                .setUserId("1")
                .setEmail("test@example.com")
                .setMessage("Test message")
                .setActionType("CREATE")
                .build();

        try (KafkaProducer<String, CreateNotificationData> producer = createProducer();
             KafkaConsumer<String, CreateNotificationData> consumer = createConsumer("test")) {

            // 2. Отправка сообщения в Kafka
            var future = producer.send(new ProducerRecord<>("test", "key", testData));
            RecordMetadata metadata = future.get();
            log.debug("Message sent to topic: {}, partition: {}, offset: {}", metadata.topic(), metadata.partition(), metadata.offset());

            // 3. Получение сообщения
            ConsumerRecords<String, CreateNotificationData> records = consumer.poll(Duration.ofSeconds(1));
            if (records.isEmpty()) {
                throw new RuntimeException("No messages received from Kafka");
            }

            var data = records.iterator().next().value();
            // 4. Сохранили в базу
            var savedData = notificationService.createNewNotification(data);
            // 5. Получили из базы
            var getNotification = notificationRepository.findByUserUid(savedData.getUserUid())
                    .orElseThrow(() -> new RuntimeException("Notification not found"));

            // 7. Отправляем email
            mailSender.send(sendTestEmail(data.getEmail(), themeOfEmail, "Test message"));

            assertEquals(1, records.count());
            // 6. Проверяем отправленное сообщение в кафку и сообщение которое сохранили в базу после получения из кафки
            assertEquals(testData.getMessage(), getNotification.getMessage());

            await().untilAsserted(() -> {
                // 8. Проверяем, что письмо получено GreenMail
                assertTrue(greenMail.waitForIncomingEmail(1));
                MimeMessage[] messages = greenMail.getReceivedMessages();
                assertEquals(1, messages.length);
                assertEquals(data.getEmail(), messages[0].getAllRecipients()[0].toString());
                assertTrue(messages[0].getContent().toString().contains("Test message"));
            });

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Проверяем кейс для обновления существующего уведомления")
    void updateNotificationTest() {

        // 1. Подготовка тестового сообщения
        CreateNotificationData testData = CreateNotificationData.newBuilder()
                .setUserId("1")
                .setEmail("test@example.com")
                .setMessage("Test message")
                .setActionType("UPDATE")
                .build();

        // 2. Сохранили в базу
        var saveNotification = notificationService.createNewNotification(testData);
        // 3. Получили из базы
        var getNotification = notificationRepository.findByUserUid(saveNotification.getUserUid());

        // 4. Подготовка тестового сообщения для update
        CreateNotificationData updateData = getNotification
                .map(notification -> CreateNotificationData.newBuilder()
                        .setUserId(notification.getUserUid())
                        .setEmail("test@example.com")
                        .setMessage("Update message")
                        .setActionType("UPDATE")
                        .build())
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        try (var producer = createProducer();
             var  consumer = createConsumer("test")) {

            // 5. Отправка сообщения в Kafka
            var future = producer.send(new ProducerRecord<>("test", "key", updateData));
            RecordMetadata metadata = future.get();
            log.debug("Message sent to topic: {}, partition: {}, offset: {}", metadata.topic(), metadata.partition(), metadata.offset());

            // 6. Получение сообщения
            ConsumerRecords<String, CreateNotificationData> records = consumer.poll(Duration.ofSeconds(1));
            if (records.isEmpty()) {
                throw new RuntimeException("No messages received from Kafka");
            }

            var data = records.iterator().next().value();
            // 7. Сохранили в базу
            var savedNotification = notificationService.updateNewNotification(data);
            // 8. Получили из базы
            var receivedNotification = notificationRepository.findByUserUid(savedNotification.getUserUid())
                    .orElseThrow(() -> new RuntimeException("Notification not found"));


            assertEquals(1, records.count());
            // 9. Проверяем отправленное сообщение в кафку и сообщение которое сохранили в базу после получения из кафки
            assertEquals(updateData.getMessage(), receivedNotification.getMessage());

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
