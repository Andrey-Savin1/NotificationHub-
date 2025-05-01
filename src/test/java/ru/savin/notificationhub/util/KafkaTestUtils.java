package ru.savin.notificationhub.util;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import ru.savin.notificationhub.CreateNotificationData;

import java.util.Collections;
import java.util.Properties;

import static ru.savin.notificationhub.TestContainersConfig.kafkaTest;
import static ru.savin.notificationhub.TestContainersConfig.schemaRegistry;

public class KafkaTestUtils {


    public static KafkaProducer<String, CreateNotificationData> createProducer() {
        Properties producerProps = new Properties();
        String schemaRegistryUrl = "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081));
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTest.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        producerProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

        return new KafkaProducer<>(producerProps);
    }

    public static KafkaConsumer<String, CreateNotificationData> createConsumer(String topic) {
        Properties consumerProps = new Properties();
        KafkaConsumer<String, CreateNotificationData> consumer = new KafkaConsumer<>(consumerProps);

        String schemaRegistryUrl = "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081));

        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTest.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Настройка ErrorHandlingDeserializer для ключей и значений
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        consumerProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
        consumerProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, KafkaAvroDeserializer.class.getName());

        consumerProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        consumerProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        consumer.subscribe(Collections.singletonList(topic));
        return consumer;
    }

}
