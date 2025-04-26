package ru.savin.notificationhub;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;
import ru.savin.notificationhub.dto.CreateNotificationData;

import java.util.Properties;

@Slf4j
@RequiredArgsConstructor
@SpringBootTest
public class TestContainersConfig {


    private static final String CONFLUENT_VERSION = "7.7.3";
    private static final Network network = Network.newNetwork();

    @Container
    public static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").withTag(CONFLUENT_VERSION))
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
                    .dependsOn(kafka);

    @Container
    public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest")
            .withNetwork(network).withNetworkMode(network.getId())
            .withNetworkAliases("postgres")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testuser");

    static {
        postgres.start();
        kafka.start();
        schemaRegistry.start();
    }


    public static KafkaProducer<String, CreateNotificationData> createProducer() {
        Properties producerProps = new Properties();
        String schemaRegistryUrl = "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081));
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        producerProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        return new KafkaProducer<>(producerProps);
    }

    public static KafkaConsumer<String, CreateNotificationData> createConsumer() {
        Properties consumerProps = new Properties();
        String schemaRegistryUrl = "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081));

        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Настройка ErrorHandlingDeserializer для ключей и значений
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        consumerProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
        consumerProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, KafkaAvroDeserializer.class.getName());

        consumerProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        consumerProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        return new KafkaConsumer<>(consumerProps);
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
                "  \"schema\": \"{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"CreateNotificationData\\\",\\\"namespace\\\":\\\"ru.savin.notificationhub.dto\\\",\\\"fields\\\":[{\\\"name\\\":\\\"userId\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"message\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"email\\\",\\\"type\\\":\\\"string\\\"}]}\"\n" +
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
