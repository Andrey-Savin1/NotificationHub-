package ru.savin.notificationhub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@RequiredArgsConstructor
@SpringBootTest
public class TestContainersConfig {


    private static final String CONFLUENT_VERSION = "7.7.3";
    private static final Network network = Network.newNetwork();

    @Container
    public static final ConfluentKafkaContainer kafkaTest = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").withTag(CONFLUENT_VERSION))
            .withListener("tc-kafka:19092")
            .withNetwork(network).withNetworkMode(network.getId())
            .withNetworkAliases("tc-kafka")
            .withReuse(true);

    @Container
    public static final GenericContainer<?> schemaRegistry =
            new GenericContainer<>("confluentinc/cp-schema-registry:7.7.3")
                    .withExposedPorts(8081)
                    .withNetwork(network).withNetworkMode(network.getId())
                    .withNetworkAliases("schemaregistry")
                    .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schemaregistry")
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://tc-kafka:19092")
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_SECURITY_PROTOCOL", "PLAINTEXT")
                    .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                    .waitingFor(Wait.forHttp("/subjects").forStatusCode(200))
                    .dependsOn(kafkaTest);

    @Container
    public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withNetwork(network).withNetworkMode(network.getId())
            .withNetworkAliases("postgres")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testuser");

    static {
        postgres.start();
        kafkaTest.start();
        schemaRegistry.start();
    }

    public static SimpleMailMessage sendTestEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        return message;
    }

    /**
     * Метод для регистрации новой схемы в schemaRegistry (при необходимости)
     * (ВАЖНО: в названии схемы должно быть -value (notification-value, myschema-value))
     */
    public static void registerSchema() {
        String schemaRegistryUrl = "http://%s:%d/subjects/notification-value/versions"
                .formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081));

        String schema = "{\n" +
                "  \"schema\": \"{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"CreateNotificationData\\\",\\\"namespace\\\":\\\"ru.savin.notificationhub.dto\\\",\\\"fields\\\":[{\\\"name\\\":\\\"userId\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"message\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"message\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"email\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"actionType\\\",\\\"type\\\":\\\"string\\\"}]}\"\n" +
                "}";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.schemaregistry.v1+json"));
        HttpEntity<String> request = new HttpEntity<>(schema, headers);
        String response = restTemplate.postForObject(schemaRegistryUrl, request, String.class);
        log.debug("Schema registration response: {}", response);

    }

    /**
     * Метод для получения схемы из schemaRegistry
     */
    public static String getSchemaById(int schemaId) {
        String schemaRegistryUrl = "http://%s:%d/subjects/notification-value/versions/latest"
                .formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081));

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(schemaRegistryUrl, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to fetch schema with ID " + schemaId + ". Status code: " + response.getStatusCode());
        }
    }

}
